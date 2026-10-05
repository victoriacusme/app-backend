package ec.nexo.auth.infrastructure.adapter.out.persistence;

import ec.nexo.auth.application.port.out.UserRepositoryPort;
import ec.nexo.auth.domain.exception.UsernameTakenException;
import ec.nexo.auth.domain.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;

    UserRepositoryAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<User> findByUsernameForUpdate(String username) {
        return repository.findWithLockByUsername(username).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public User save(User user) {
        try {
            return toDomain(repository.saveAndFlush(toEntity(user)));
        } catch (DataIntegrityViolationException e) {
            // Carrera entre dos registros con el mismo username: lo resuelve la restricción UNIQUE.
            throw new UsernameTakenException(user.username());
        }
    }

    private static UserEntity toEntity(User user) {
        return new UserEntity(user.id(), user.username(), user.customerId(), user.passwordHash(),
                user.status(), user.failedAttempts(), user.createdAt());
    }

    private static User toDomain(UserEntity entity) {
        return User.restore(entity.getId(), entity.getUsername(), entity.getCustomerId(), entity.getPasswordHash(),
                entity.getStatus(), entity.getFailedAttempts(), entity.getCreatedAt());
    }
}
