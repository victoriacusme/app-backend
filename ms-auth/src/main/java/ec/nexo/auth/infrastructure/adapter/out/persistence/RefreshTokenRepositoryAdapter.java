package ec.nexo.auth.infrastructure.adapter.out.persistence;

import ec.nexo.auth.application.port.out.RefreshTokenRepositoryPort;
import ec.nexo.auth.domain.model.RefreshToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository repository;

    RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(RefreshTokenRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
        return repository.findWithLockByTokenHash(tokenHash).map(RefreshTokenRepositoryAdapter::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return toDomain(repository.save(toEntity(token)));
    }

    @Override
    public void revokeAllByUserId(UUID userId) {
        repository.revokeAllByUserId(userId);
    }

    private static RefreshTokenEntity toEntity(RefreshToken token) {
        return new RefreshTokenEntity(token.id(), token.userId(), token.tokenHash(), token.deviceId(),
                token.expiresAt(), token.isRevoked(), token.createdAt());
    }

    private static RefreshToken toDomain(RefreshTokenEntity entity) {
        return RefreshToken.restore(entity.getId(), entity.getUserId(), entity.getTokenHash(), entity.getDeviceId(),
                entity.getExpiresAt(), entity.isRevoked(), entity.getCreatedAt());
    }
}
