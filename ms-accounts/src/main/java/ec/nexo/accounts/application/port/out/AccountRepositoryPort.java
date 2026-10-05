package ec.nexo.accounts.application.port.out;

import ec.nexo.accounts.domain.model.Account;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepositoryPort {

    List<Account> findByCustomerId(UUID customerId);

    Optional<Account> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Account> findDefaultByCustomerId(UUID customerId);

    /** @throws DuplicateDefaultAccountException si el cliente ya tiene una cuenta por defecto */
    Account save(Account account);

    String nextAccountNumber();

    /**
     * Obtiene y bloquea las cuentas (SELECT ... FOR UPDATE) hasta el fin de la transacción.
     * Las bloquea siempre en orden de id para que dos transferencias cruzadas no provoquen un deadlock.
     */
    List<Account> findAllByIdForUpdate(Collection<UUID> ids);

    void updateBalance(Account account);
}
