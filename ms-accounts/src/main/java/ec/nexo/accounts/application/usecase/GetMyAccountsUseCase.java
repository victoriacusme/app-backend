package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.application.port.out.AccountRepositoryPort;
import ec.nexo.accounts.domain.model.Account;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class GetMyAccountsUseCase {

    private final AccountRepositoryPort accounts;

    public GetMyAccountsUseCase(AccountRepositoryPort accounts) {
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<Account> execute(UUID customerId) {
        return accounts.findByCustomerId(customerId);
    }
}
