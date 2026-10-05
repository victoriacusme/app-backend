package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.application.port.out.AccountRepositoryPort;
import ec.nexo.accounts.application.port.out.DuplicateDefaultAccountException;
import ec.nexo.accounts.domain.model.Account;

import java.time.Clock;
import java.util.UUID;

/**
 * Abre la cuenta por defecto de un cliente nuevo (llamado por ms-auth en el onboarding).
 * Es idempotente por {@code customerId}: si ms-auth reintenta, devuelve la cuenta ya creada.
 */
public class OpenDefaultAccountUseCase {

    private final AccountRepositoryPort accounts;
    private final Clock clock;

    public OpenDefaultAccountUseCase(AccountRepositoryPort accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    public Result execute(UUID customerId) {
        var existing = accounts.findDefaultByCustomerId(customerId);
        if (existing.isPresent()) {
            return new Result(existing.get(), false);
        }
        try {
            Account account = Account.openDefault(customerId, accounts.nextAccountNumber(), clock.instant());
            return new Result(accounts.save(account), true);
        } catch (DuplicateDefaultAccountException e) {
            // Dos llamadas simultáneas: ganó la otra, se devuelve su cuenta.
            return new Result(accounts.findDefaultByCustomerId(customerId).orElseThrow(() -> e), false);
        }
    }

    public record Result(Account account, boolean created) {
    }
}
