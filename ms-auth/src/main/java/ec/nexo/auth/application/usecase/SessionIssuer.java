package ec.nexo.auth.application.usecase;

import ec.nexo.auth.application.port.out.AccessTokenIssuerPort;
import ec.nexo.auth.application.port.out.RefreshTokenRepositoryPort;
import ec.nexo.auth.domain.model.AuthTokens;
import ec.nexo.auth.domain.model.RefreshToken;
import ec.nexo.auth.domain.model.User;

import java.time.Clock;
import java.time.Duration;

/** Abre una sesión: emite el access token y persiste un refresh token nuevo. */
public class SessionIssuer {

    private final AccessTokenIssuerPort accessTokenIssuer;
    private final RefreshTokenRepositoryPort refreshTokens;
    private final Duration refreshTokenTtl;
    private final Clock clock;

    public SessionIssuer(AccessTokenIssuerPort accessTokenIssuer, RefreshTokenRepositoryPort refreshTokens,
                         Duration refreshTokenTtl, Clock clock) {
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokens = refreshTokens;
        this.refreshTokenTtl = refreshTokenTtl;
        this.clock = clock;
    }

    public AuthTokens open(User user, String deviceId) {
        var accessToken = accessTokenIssuer.issue(user);
        String rawRefreshToken = RefreshToken.generateValue();
        refreshTokens.save(RefreshToken.issue(user.id(), RefreshToken.hash(rawRefreshToken), deviceId,
                clock.instant(), refreshTokenTtl));
        return new AuthTokens(accessToken.value(), accessToken.ttl(), rawRefreshToken, refreshTokenTtl);
    }
}
