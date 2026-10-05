package ec.nexo.auth.application.usecase;

import ec.nexo.auth.application.port.out.RefreshTokenRepositoryPort;
import ec.nexo.auth.domain.model.RefreshToken;
import org.springframework.transaction.annotation.Transactional;

/** Revoca el refresh token. Es idempotente y no revela si el token existía. */
public class LogoutUseCase {

    private final RefreshTokenRepositoryPort refreshTokens;

    public LogoutUseCase(RefreshTokenRepositoryPort refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public void execute(String rawRefreshToken) {
        refreshTokens.findByTokenHash(RefreshToken.hash(rawRefreshToken))
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokens.save(token);
                });
    }
}
