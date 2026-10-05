package ec.nexo.auth.infrastructure.adapter.in.web.dto;

import ec.nexo.auth.domain.model.AuthTokens;

public record TokenResponse(String accessToken, String tokenType, long expiresIn,
                            String refreshToken, long refreshExpiresIn) {

    public static TokenResponse from(AuthTokens tokens) {
        return new TokenResponse(tokens.accessToken(), "Bearer", tokens.accessTokenTtl().toSeconds(),
                tokens.refreshToken(), tokens.refreshTokenTtl().toSeconds());
    }
}
