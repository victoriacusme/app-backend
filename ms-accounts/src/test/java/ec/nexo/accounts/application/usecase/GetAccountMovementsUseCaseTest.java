package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.application.port.out.AccountRepositoryPort;
import ec.nexo.accounts.application.port.out.MovementRepositoryPort;
import ec.nexo.accounts.domain.exception.AccountNotFoundException;
import ec.nexo.accounts.domain.model.Account;
import ec.nexo.accounts.domain.model.Movement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAccountMovementsUseCaseTest {

    @Mock
    private AccountRepositoryPort accounts;
    @Mock
    private MovementRepositoryPort movements;
    @InjectMocks
    private GetAccountMovementsUseCase useCase;

    private final UUID customerId = UUID.randomUUID();
    private Account account;

    @BeforeEach
    void setUp() {
        account = Fixtures.account(customerId);
    }

    @Test
    void siHayMasResultadosDevuelveLaPaginaYElCursorDelUltimo() {
        when(accounts.findByIdAndCustomerId(account.id(), customerId)).thenReturn(Optional.of(account));
        List<Movement> found = movementsOf(4);
        when(movements.findPage(account.id(), null, 4)).thenReturn(found);

        MovementPage page = useCase.execute(customerId, account.id(), null, 3);

        assertThat(page.items()).containsExactlyElementsOf(found.subList(0, 3));
        assertThat(page.next()).isEqualTo(MovementCursor.of(found.get(2)));
    }

    @Test
    void enLaUltimaPaginaNoHayCursor() {
        when(accounts.findByIdAndCustomerId(account.id(), customerId)).thenReturn(Optional.of(account));
        List<Movement> found = movementsOf(2);
        MovementCursor after = MovementCursor.of(Fixtures.movement(account.id(), 0));
        when(movements.findPage(account.id(), after, 4)).thenReturn(found);

        MovementPage page = useCase.execute(customerId, account.id(), after, 3);

        assertThat(page.items()).containsExactlyElementsOf(found);
        assertThat(page.next()).isNull();
    }

    @Test
    void usaVeinteComoTamanoPorDefectoYLimitaElMaximoACincuenta() {
        when(accounts.findByIdAndCustomerId(account.id(), customerId)).thenReturn(Optional.of(account));
        when(movements.findPage(eq(account.id()), any(), anyInt())).thenReturn(List.of());

        useCase.execute(customerId, account.id(), null, null);
        useCase.execute(customerId, account.id(), null, 500);
        useCase.execute(customerId, account.id(), null, 0);

        verify(movements).findPage(account.id(), null, 21);
        verify(movements).findPage(account.id(), null, 51);
        verify(movements).findPage(account.id(), null, 2);
    }

    @Test
    void noConsultaMovimientosDeUnaCuentaAjena() {
        UUID foreignAccountId = UUID.randomUUID();
        when(accounts.findByIdAndCustomerId(foreignAccountId, customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(customerId, foreignAccountId, null, 20))
                .isInstanceOf(AccountNotFoundException.class);

        verifyNoInteractions(movements);
    }

    private List<Movement> movementsOf(int count) {
        return IntStream.range(0, count).mapToObj(i -> Fixtures.movement(account.id(), i)).toList();
    }
}
