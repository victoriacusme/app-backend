package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.application.port.out.AccountRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMyAccountsUseCaseTest {

    @Mock
    private AccountRepositoryPort accounts;
    @InjectMocks
    private GetMyAccountsUseCase useCase;

    @Test
    void devuelveLasCuentasDelCliente() {
        UUID customerId = UUID.randomUUID();
        var owned = List.of(Fixtures.account(customerId), Fixtures.account(customerId));
        when(accounts.findByCustomerId(customerId)).thenReturn(owned);

        assertThat(useCase.execute(customerId)).isEqualTo(owned);
    }
}
