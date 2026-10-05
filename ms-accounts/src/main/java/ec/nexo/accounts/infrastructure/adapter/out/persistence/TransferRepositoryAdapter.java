package ec.nexo.accounts.infrastructure.adapter.out.persistence;

import ec.nexo.accounts.application.port.out.TransferRepositoryPort;
import ec.nexo.accounts.domain.exception.IdempotencyKeyReusedException;
import ec.nexo.accounts.domain.model.Transfer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class TransferRepositoryAdapter implements TransferRepositoryPort {

    private final TransferJpaRepository repository;

    TransferRepositoryAdapter(TransferJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Transfer> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey) {
        return repository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)
                .map(TransferRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Transfer> findByIdAndCustomerId(UUID id, UUID customerId) {
        return repository.findByIdAndCustomerId(id, customerId).map(TransferRepositoryAdapter::toDomain);
    }

    @Override
    public Transfer save(Transfer transfer) {
        try {
            return toDomain(repository.saveAndFlush(toEntity(transfer)));
        } catch (DataIntegrityViolationException e) {
            // Misma clave enviada a la vez con datos distintos (sobre otras cuentas): la resuelve el UNIQUE.
            throw new IdempotencyKeyReusedException();
        }
    }

    private static TransferEntity toEntity(Transfer transfer) {
        return new TransferEntity(transfer.id(), transfer.customerId(), transfer.idempotencyKey(),
                transfer.requestHash(), transfer.sourceAccountId(), transfer.targetAccountId(), transfer.amount(),
                transfer.currency(), transfer.description(), transfer.status(), transfer.createdAt());
    }

    private static Transfer toDomain(TransferEntity entity) {
        return new Transfer(entity.getId(), entity.getCustomerId(), entity.getIdempotencyKey(),
                entity.getRequestHash(), entity.getSourceAccountId(), entity.getTargetAccountId(), entity.getAmount(),
                entity.getCurrency(), entity.getDescription(), entity.getStatus(), entity.getCreatedAt());
    }
}
