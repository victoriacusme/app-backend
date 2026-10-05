package ec.nexo.auth.application.port.out;

import ec.nexo.auth.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepositoryPort {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Obtiene el token bloqueando la fila, para que dos rotaciones simultáneas no usen el mismo token. */
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    RefreshToken save(RefreshToken refreshToken);

    void revokeAllByUserId(UUID userId);
}
