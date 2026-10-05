package ec.nexo.accounts.infrastructure.adapter.out.persistence;

import ec.nexo.accounts.application.port.out.AccountRepositoryPort;
import ec.nexo.accounts.application.port.out.DuplicateDefaultAccountException;
import ec.nexo.accounts.domain.model.Account;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class AccountRepositoryAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository repository;

    AccountRepositoryAdapter(AccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Account> findByCustomerId(UUID customerId) {
        return repository.findByCustomerIdOrderByIsDefaultDescCreatedAtAsc(customerId).stream()
                .map(AccountRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<Account> findByIdAndCustomerId(UUID id, UUID customerId) {
        return repository.findByIdAndCustomerId(id, customerId).map(AccountRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Account> findDefaultByCustomerId(UUID customerId) {
        return repository.findByCustomerIdAndIsDefaultTrue(customerId).map(AccountRepositoryAdapter::toDomain);
    }

    @Override
    public Account save(Account account) {
        try {
            return toDomain(repository.saveAndFlush(toEntity(account)));
        } catch (DataIntegrityViolationException e) {
            // Carrera entre dos aperturas para el mismo cliente: la resuelve el índice único parcial.
            throw new DuplicateDefaultAccountException(e);
        }
    }

    @Override
    public String nextAccountNumber() {
        return String.valueOf(repository.nextAccountNumber());
    }

    @Override
    public List<Account> findAllByIdForUpdate(Collection<UUID> ids) {
        return repository.findAllByIdInForUpdate(ids).stream().map(AccountRepositoryAdapter::toDomain).toList();
    }

    /** La entidad ya está en el contexto de persistencia (la cargó el bloqueo): el cambio se guarda al confirmar. */
    @Override
    public void updateBalance(Account account) {
        AccountEntity entity = repository.findById(account.id())
                .orElseThrow(() -> new IllegalStateException("La cuenta " + account.id() + " no existe"));
        entity.changeBalance(account.balance());
    }

    private static AccountEntity toEntity(Account account) {
        return new AccountEntity(account.id(), account.customerId(), account.number(), account.type(),
                account.currency(), account.balance(), account.status(), account.alias(), account.isDefault(),
                account.createdAt());
    }

    private static Account toDomain(AccountEntity entity) {
        return Account.restore(entity.getId(), entity.getCustomerId(), entity.getNumber(), entity.getType(),
                entity.getCurrency(), entity.getBalance(), entity.getStatus(), entity.getAlias(), entity.isDefault(),
                entity.getCreatedAt());
    }
}
