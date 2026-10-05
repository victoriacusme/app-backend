package ec.nexo.auth.domain.model;

import java.time.Duration;

/** Par de tokens entregado al cliente tras autenticarse. */
public record AuthTokens(String accessToken, Duration accessTokenTtl, String refreshToken, Duration refreshTokenTtl) {
}
