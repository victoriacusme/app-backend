package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.application.port.out.AccountRepositoryPort;
import ec.nexo.accounts.domain.exception.AccountNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMyAccountUseCaseTest {

    @Mock
    private AccountRepositoryPort accounts;
    @InjectMocks
    private GetMyAccountUseCase useCase;

    @Test
    void devuelveLaCuentaSiPerteneceAlCliente() {
        UUID customerId = UUID.randomUUID();
        var account = Fixtures.account(customerId);
        when(accounts.findByIdAndCustomerId(account.id(), customerId)).thenReturn(Optional.of(account));

        assertThat(useCase.execute(customerId, account.id())).isSameAs(account);
    }

    @Test
    void unaCuentaAjenaSeRespondeComoInexistente() {
        UUID customerId = UUID.randomUUID();
        UUID foreignAccountId = UUID.randomUUID();
        when(accounts.findByIdAndCustomerId(foreignAccountId, customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(customerId, foreignAccountId))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
