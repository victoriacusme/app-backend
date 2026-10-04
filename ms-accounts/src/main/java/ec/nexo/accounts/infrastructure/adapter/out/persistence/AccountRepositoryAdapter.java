package ec.nexo.accounts.infrastructure.adapter.out.persistence;

import ec.nexo.accounts.application.port.out.AccountRepositoryPort;
import ec.nexo.accounts.application.port.out.DuplicateDefaultAccountException;
import ec.nexo.accounts.domain.model.Account;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

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
