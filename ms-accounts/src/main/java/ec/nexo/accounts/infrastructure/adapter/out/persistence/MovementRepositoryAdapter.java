package ec.nexo.accounts.infrastructure.adapter.out.persistence;

import ec.nexo.accounts.application.port.out.MovementRepositoryPort;
import ec.nexo.accounts.application.usecase.MovementCursor;
import ec.nexo.accounts.domain.model.Movement;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
class MovementRepositoryAdapter implements MovementRepositoryPort {

    private final MovementJpaRepository repository;

    MovementRepositoryAdapter(MovementJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Movement> findPage(UUID accountId, MovementCursor after, int limit) {
        var entities = after == null
                ? repository.findFirstPage(accountId, limit)
                : repository.findPageAfter(accountId, after.bookedAt(), after.id(), limit);
        return entities.stream().map(MovementRepositoryAdapter::toDomain).toList();
    }

    private static Movement toDomain(MovementEntity entity) {
        return new Movement(entity.getId(), entity.getAccountId(), entity.getType(), entity.getAmount(),
                entity.getBalanceAfter(), entity.getDescription(), entity.getBookedAt());
    }
}
