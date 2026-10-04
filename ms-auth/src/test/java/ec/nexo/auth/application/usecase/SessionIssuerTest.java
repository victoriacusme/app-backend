package ec.nexo.auth.application.usecase;

import ec.nexo.auth.application.port.out.AccessTokenIssuerPort;
import ec.nexo.auth.application.port.out.AccessTokenIssuerPort.IssuedAccessToken;
import ec.nexo.auth.application.port.out.RefreshTokenRepositoryPort;
import ec.nexo.auth.domain.model.AuthTokens;
import ec.nexo.auth.domain.model.RefreshToken;
import ec.nexo.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionIssuerTest {

    private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");
    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    @Mock
    private AccessTokenIssuerPort accessTokenIssuer;
    @Mock
    private RefreshTokenRepositoryPort refreshTokens;
    @Mock
    private Clock clock;
    @Captor
    private ArgumentCaptor<RefreshToken> savedToken;

    private SessionIssuer sessionIssuer;

    @BeforeEach
    void setUp() {
        sessionIssuer = new SessionIssuer(accessTokenIssuer, refreshTokens, REFRESH_TTL, clock);
    }

    @Test
    void emiteElAccessTokenYGuardaSoloElHashDelRefreshToken() {
        User ana = User.register("ana", UUID.randomUUID(), "hash", NOW);
        when(accessTokenIssuer.issue(ana)).thenReturn(new IssuedAccessToken("access", Duration.ofMinutes(15)));
        when(clock.instant()).thenReturn(NOW);

        AuthTokens tokens = sessionIssuer.open(ana, "pixel-8");

        assertThat(tokens.accessToken()).isEqualTo("access");
        assertThat(tokens.refreshToken()).isNotBlank();
        verify(refreshTokens).save(savedToken.capture());
        RefreshToken stored = savedToken.getValue();
        assertThat(stored.tokenHash())
                .isEqualTo(RefreshToken.hash(tokens.refreshToken()))
                .isNotEqualTo(tokens.refreshToken());
        assertThat(stored.userId()).isEqualTo(ana.id());
        assertThat(stored.deviceId()).isEqualTo("pixel-8");
        assertThat(stored.expiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));
    }
}
