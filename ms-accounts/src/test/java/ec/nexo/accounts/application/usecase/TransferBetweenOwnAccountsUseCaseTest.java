package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.application.port.out.AccountRepositoryPort;
import ec.nexo.accounts.application.port.out.MovementRepositoryPort;
import ec.nexo.accounts.application.port.out.NotificationPort;
import ec.nexo.accounts.application.port.out.TransferNotification;
import ec.nexo.accounts.application.port.out.TransferRepositoryPort;
import ec.nexo.accounts.application.usecase.TransferBetweenOwnAccountsUseCase.TransferCommand;
import ec.nexo.accounts.domain.exception.AccountNotActiveException;
import ec.nexo.accounts.domain.exception.AccountNotFoundException;
import ec.nexo.accounts.domain.exception.AccountNotOwnedException;
import ec.nexo.accounts.domain.exception.IdempotencyKeyReusedException;
import ec.nexo.accounts.domain.exception.InsufficientFundsException;
import ec.nexo.accounts.domain.exception.InvalidAmountException;
import ec.nexo.accounts.domain.exception.SameAccountTransferException;
import ec.nexo.accounts.domain.model.Account;
import ec.nexo.accounts.domain.model.Movement;
import ec.nexo.accounts.domain.model.MovementType;
import ec.nexo.accounts.domain.model.Transfer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferBetweenOwnAccountsUseCaseTest {

    private static final String KEY = "3f1c2a90-key";

    @Mock
    private AccountRepositoryPort accounts;
    @Mock
    private MovementRepositoryPort movements;
    @Mock
    private TransferRepositoryPort transfers;
    @Mock
    private NotificationPort notifications;
    @Mock
    private Clock clock;
    @Captor
    private ArgumentCaptor<List<Movement>> savedMovements;

    private TransferBetweenOwnAccountsUseCase useCase;
    private final UUID ana = UUID.randomUUID();
    private Account source;
    private Account target;

    @BeforeEach
    void setUp() {
        useCase = new TransferBetweenOwnAccountsUseCase(accounts, movements, transfers, notifications, clock);
        source = Fixtures.account(ana, "2200014521", "100.00");
        target = Fixtures.account(ana, "2200017834", "20.00");
    }

    @Test
    void debitaAcreditaRegistraDosMovimientosYNotifica() {
        givenNoPreviousTransfer();
        givenLocked(source, target);
        when(clock.instant()).thenReturn(Fixtures.NOW);
        when(transfers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(command("30.50", "Ahorro del mes"));

        assertThat(result.replayed()).isFalse();
        Transfer transfer = result.transfer();
        assertThat(transfer.amount()).isEqualByComparingTo("30.50");
        assertThat(transfer.idempotencyKey()).isEqualTo(KEY);
        assertThat(source.balance()).isEqualByComparingTo("69.50");
        assertThat(target.balance()).isEqualByComparingTo("50.50");

        InOrder order = inOrder(accounts, transfers, movements, notifications);
        order.verify(accounts).findAllByIdForUpdate(Set.of(source.id(), target.id()));
        order.verify(transfers).save(transfer);
        order.verify(accounts).updateBalance(source);
        order.verify(accounts).updateBalance(target);
        order.verify(movements).saveAll(savedMovements.capture());
        order.verify(notifications).transferCompleted(new TransferNotification(ana, transfer.id(),
                new BigDecimal("30.50"), "USD", "****7834"));

        assertThat(savedMovements.getValue()).satisfiesExactly(
                debit -> {
                    assertThat(debit.accountId()).isEqualTo(source.id());
                    assertThat(debit.type()).isEqualTo(MovementType.DEBIT);
                    assertThat(debit.balanceAfter()).isEqualByComparingTo("69.50");
                    assertThat(debit.description()).isEqualTo("Transferencia a ****7834 - Ahorro del mes");
                    assertThat(debit.transferId()).isEqualTo(transfer.id());
                },
                credit -> {
                    assertThat(credit.accountId()).isEqualTo(target.id());
                    assertThat(credit.type()).isEqualTo(MovementType.CREDIT);
                    assertThat(credit.balanceAfter()).isEqualByComparingTo("50.50");
                    assertThat(credit.description()).isEqualTo("Transferencia desde ****4521 - Ahorro del mes");
                });
    }

    @Test
    void unReintentoConLaMismaClaveDevuelveLaTransferenciaOriginalSinMoverDinero() {
        Transfer original = previousTransfer("30.50", null);
        when(transfers.findByCustomerIdAndIdempotencyKey(ana, KEY)).thenReturn(Optional.of(original));

        // "30.5" y "30.50" son el mismo monto: debe reconocerse como el mismo reintento.
        var result = useCase.execute(command("30.5", null));

        assertThat(result.replayed()).isTrue();
        assertThat(result.transfer()).isSameAs(original);
        verify(accounts, never()).findAllByIdForUpdate(any());
        verifyNoInteractions(movements, notifications);
    }

    @Test
    void siOtraSolicitudConLaMismaClaveTerminoMientrasEsperabaElBloqueoDevuelveEsa() {
        Transfer completedMeanwhile = previousTransfer("30.50", null);
        when(transfers.findByCustomerIdAndIdempotencyKey(ana, KEY))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(completedMeanwhile));
        givenLocked(source, target);

        var result = useCase.execute(command("30.50", null));

        assertThat(result.replayed()).isTrue();
        assertThat(source.balance()).isEqualByComparingTo("100.00");
        verify(transfers, never()).save(any());
        verifyNoInteractions(movements, notifications);
    }

    @Test
    void laMismaClaveConOtroMontoEsUnConflicto() {
        when(transfers.findByCustomerIdAndIdempotencyKey(ana, KEY))
                .thenReturn(Optional.of(previousTransfer("30.50", null)));

        assertThatThrownBy(() -> useCase.execute(command("99.00", null)))
                .isInstanceOf(IdempotencyKeyReusedException.class);

        verifyNoInteractions(movements, notifications);
    }

    @Test
    void saldoInsuficienteNoGuardaNada() {
        givenNoPreviousTransfer();
        givenLocked(source, target);

        assertThatThrownBy(() -> useCase.execute(command("100.01", null)))
                .isInstanceOf(InsufficientFundsException.class);

        verify(transfers, never()).save(any());
        verify(accounts, never()).updateBalance(any());
        verifyNoInteractions(movements, notifications);
    }

    @Test
    void laCuentaDestinoDeOtroClienteDa403() {
        target = Fixtures.account(UUID.randomUUID(), "2200020117", "0.00");
        givenNoPreviousTransfer();
        givenLocked(source, target);

        assertThatThrownBy(() -> useCase.execute(command("10.00", null)))
                .isInstanceOf(AccountNotOwnedException.class);

        assertThat(source.balance()).isEqualByComparingTo("100.00");
        verify(transfers, never()).save(any());
    }

    @Test
    void unaCuentaInexistenteDa404() {
        givenNoPreviousTransfer();
        when(accounts.findAllByIdForUpdate(Set.of(source.id(), target.id()))).thenReturn(List.of(source));

        assertThatThrownBy(() -> useCase.execute(command("10.00", null)))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void unaCuentaBloqueadaNoPuedeRecibir() {
        target = Fixtures.blockedAccount(ana, "0.00");
        givenNoPreviousTransfer();
        givenLocked(source, target);

        assertThatThrownBy(() -> useCase.execute(command("10.00", null)))
                .isInstanceOf(AccountNotActiveException.class);

        verify(transfers, never()).save(any());
    }

    @Test
    void validaElMontoYLasCuentasAntesDeTocarLaBaseDeDatos() {
        assertThatThrownBy(() -> useCase.execute(command("0", null))).isInstanceOf(InvalidAmountException.class);
        assertThatThrownBy(() -> useCase.execute(command("-5", null))).isInstanceOf(InvalidAmountException.class);
        assertThatThrownBy(() -> useCase.execute(command("1.001", null))).isInstanceOf(InvalidAmountException.class);
        assertThatThrownBy(() -> useCase.execute(new TransferCommand(ana, KEY, source.id(), source.id(),
                new BigDecimal("10"), null))).isInstanceOf(SameAccountTransferException.class);

        verifyNoInteractions(accounts, transfers, movements, notifications);
    }

    private TransferCommand command(String amount, String description) {
        return new TransferCommand(ana, KEY, source.id(), target.id(), new BigDecimal(amount), description);
    }

    private void givenNoPreviousTransfer() {
        when(transfers.findByCustomerIdAndIdempotencyKey(ana, KEY)).thenReturn(Optional.empty());
    }

    private void givenLocked(Account... locked) {
        when(accounts.findAllByIdForUpdate(Set.of(source.id(), target.id()))).thenReturn(List.of(locked));
    }

    private Transfer previousTransfer(String amount, String description) {
        BigDecimal value = new BigDecimal(amount);
        return Transfer.completed(ana, KEY, Transfer.fingerprint(source.id(), target.id(), value, description),
                source, target, value, description, Fixtures.NOW);
    }
}
