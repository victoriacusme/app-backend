package ec.nexo.auth.application.port.out;

import ec.nexo.auth.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    Optional<User> findById(UUID id);

    /** Obtiene el usuario bloqueando la fila hasta el fin de la transacción (contador de intentos fallidos). */
    Optional<User> findByUsernameForUpdate(String username);

    boolean existsByUsername(String username);

    /** @throws ec.nexo.auth.domain.exception.UsernameTakenException si el username ya existe */
    User save(User user);
}
