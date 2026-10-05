package ec.nexo.auth.application.usecase;

import ec.nexo.auth.application.port.out.RefreshTokenRepositoryPort;
import ec.nexo.auth.application.port.out.UserRepositoryPort;
import ec.nexo.auth.domain.exception.AuthException;
import ec.nexo.auth.domain.exception.InvalidRefreshTokenException;
import ec.nexo.auth.domain.exception.UserLockedException;
import ec.nexo.auth.domain.model.AuthTokens;
import ec.nexo.auth.domain.model.RefreshToken;
import ec.nexo.auth.domain.model.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Rotación del refresh token: cada uso revoca el token presentado y entrega uno nuevo.
 * Si llega un token ya revocado se asume que fue robado y se revocan todas las sesiones del usuario.
 */
public class RefreshTokenUseCase {

    private final RefreshTokenRepositoryPort refreshTokens;
    private final UserRepositoryPort users;
    private final SessionIssuer sessionIssuer;
    private final Clock clock;

    public RefreshTokenUseCase(RefreshTokenRepositoryPort refreshTokens, UserRepositoryPort users,
                               SessionIssuer sessionIssuer, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.sessionIssuer = sessionIssuer;
        this.clock = clock;
    }

    // La revocación masiva por reutilización debe persistir aunque se responda con error.
    @Transactional(noRollbackFor = AuthException.class)
    public AuthTokens execute(String rawRefreshToken) {
        RefreshToken current = refreshTokens.findByTokenHashForUpdate(RefreshToken.hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isRevoked()) {
            refreshTokens.revokeAllByUserId(current.userId());
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpired(clock.instant())) {
            throw new InvalidRefreshTokenException();
        }

        User user = users.findById(current.userId()).orElseThrow(InvalidRefreshTokenException::new);
        current.revoke();
        refreshTokens.save(current);
        if (user.isLocked()) {
            throw new UserLockedException();
        }
        return sessionIssuer.open(user, current.deviceId());
    }
}
