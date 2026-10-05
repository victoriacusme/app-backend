package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.application.port.out.AccountRepositoryPort;
import ec.nexo.accounts.domain.exception.AccountNotFoundException;
import ec.nexo.accounts.domain.model.Account;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class GetMyAccountUseCase {

    private final AccountRepositoryPort accounts;

    public GetMyAccountUseCase(AccountRepositoryPort accounts) {
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public Account execute(UUID customerId, UUID accountId) {
        return accounts.findByIdAndCustomerId(accountId, customerId).orElseThrow(AccountNotFoundException::new);
    }
}
