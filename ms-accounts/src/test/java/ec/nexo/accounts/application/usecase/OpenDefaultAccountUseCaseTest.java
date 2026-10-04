package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.application.port.out.AccountRepositoryPort;
import ec.nexo.accounts.application.port.out.DuplicateDefaultAccountException;
import ec.nexo.accounts.domain.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenDefaultAccountUseCaseTest {

    @Mock
    private AccountRepositoryPort accounts;
    @Mock
    private Clock clock;
    @Captor
    private ArgumentCaptor<Account> saved;

    private OpenDefaultAccountUseCase useCase;
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new OpenDefaultAccountUseCase(accounts, clock);
    }

    @Test
    void abreUnaCuentaNuevaSiElClienteNoTiene() {
        when(accounts.findDefaultByCustomerId(customerId)).thenReturn(Optional.empty());
        when(accounts.nextAccountNumber()).thenReturn("2200100007");
        when(clock.instant()).thenReturn(Fixtures.NOW);
        when(accounts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(customerId);

        assertThat(result.created()).isTrue();
        verify(accounts).save(saved.capture());
        assertThat(saved.getValue().customerId()).isEqualTo(customerId);
        assertThat(saved.getValue().number()).isEqualTo("2200100007");
        assertThat(saved.getValue().isDefault()).isTrue();
        assertThat(saved.getValue().createdAt()).isEqualTo(Fixtures.NOW);
    }

    @Test
    void esIdempotenteSiLaCuentaYaExiste() {
        Account existing = Fixtures.account(customerId);
        when(accounts.findDefaultByCustomerId(customerId)).thenReturn(Optional.of(existing));

        var result = useCase.execute(customerId);

        assertThat(result.created()).isFalse();
        assertThat(result.account()).isSameAs(existing);
        verify(accounts, never()).save(any());
    }

    @Test
    void siOtraPeticionLaCreoAlMismoTiempoDevuelveEsaCuenta() {
        Account createdByOther = Fixtures.account(customerId);
        when(accounts.findDefaultByCustomerId(customerId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(createdByOther));
        when(accounts.nextAccountNumber()).thenReturn("2200100008");
        when(clock.instant()).thenReturn(Fixtures.NOW);
        when(accounts.save(any())).thenThrow(new DuplicateDefaultAccountException(null));

        var result = useCase.execute(customerId);

        assertThat(result.created()).isFalse();
        assertThat(result.account()).isSameAs(createdByOther);
    }
}
