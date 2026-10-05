package ec.nexo.auth.application.usecase;

import ec.nexo.auth.application.port.out.RefreshTokenRepositoryPort;
import ec.nexo.auth.application.port.out.UserRepositoryPort;
import ec.nexo.auth.domain.exception.InvalidRefreshTokenException;
import ec.nexo.auth.domain.exception.UserLockedException;
import ec.nexo.auth.domain.model.AuthTokens;
import ec.nexo.auth.domain.model.LockoutPolicy;
import ec.nexo.auth.domain.model.RefreshToken;
import ec.nexo.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    private static final String RAW_TOKEN = "refresh-en-claro";
    private static final String TOKEN_HASH = RefreshToken.hash(RAW_TOKEN);
    private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

    @Mock
    private RefreshTokenRepositoryPort refreshTokens;
    @Mock
    private UserRepositoryPort users;
    @Mock
    private SessionIssuer sessionIssuer;
    @Mock
    private Clock clock;

    private RefreshTokenUseCase refreshUseCase;
    private User ana;

    @BeforeEach
    void setUp() {
        refreshUseCase = new RefreshTokenUseCase(refreshTokens, users, sessionIssuer, clock);
        ana = User.register("ana", UUID.randomUUID(), "hash", NOW);
    }

    @Test
    void rotaElTokenRevocandoElActualYAbriendoSesionEnElMismoDispositivo() {
        RefreshToken current = tokenOfAna(NOW.minus(Duration.ofDays(1)));
        AuthTokens rotated = new AuthTokens("access", Duration.ofMinutes(15), "nuevo", Duration.ofDays(7));
        when(refreshTokens.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(current));
        when(clock.instant()).thenReturn(NOW);
        when(users.findById(ana.id())).thenReturn(Optional.of(ana));
        when(sessionIssuer.open(ana, "pixel-8")).thenReturn(rotated);

        AuthTokens result = refreshUseCase.execute(RAW_TOKEN);

        assertThat(result).isSameAs(rotated);
        assertThat(current.isRevoked()).isTrue();
        verify(refreshTokens).save(current);
    }

    @Test
    void reutilizarUnTokenRevocadoRevocaTodasLasSesionesDelUsuario() {
        RefreshToken revoked = tokenOfAna(NOW);
        revoked.revoke();
        when(refreshTokens.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshUseCase.execute(RAW_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokens).revokeAllByUserId(ana.id());
        verifyNoInteractions(sessionIssuer);
    }

    @Test
    void rechazaUnTokenExpirado() {
        RefreshToken expired = tokenOfAna(NOW.minus(Duration.ofDays(7)));
        when(refreshTokens.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(expired));
        when(clock.instant()).thenReturn(NOW);

        assertThatThrownBy(() -> refreshUseCase.execute(RAW_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokens, never()).save(any());
        verifyNoInteractions(sessionIssuer);
    }

    @Test
    void rechazaUnTokenDesconocido() {
        when(refreshTokens.findByTokenHashForUpdate(RefreshToken.hash("no-existe"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshUseCase.execute("no-existe"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(users, sessionIssuer);
    }

    @Test
    void usuarioBloqueadoPierdeElTokenYNoRecibeUnoNuevo() {
        for (int i = 0; i < 5; i++) {
            ana.registerFailedAttempt(new LockoutPolicy(5));
        }
        RefreshToken current = tokenOfAna(NOW);
        when(refreshTokens.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(current));
        when(clock.instant()).thenReturn(NOW);
        when(users.findById(ana.id())).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> refreshUseCase.execute(RAW_TOKEN))
                .isInstanceOf(UserLockedException.class);

        assertThat(current.isRevoked()).isTrue();
        verify(refreshTokens).save(current);
        verifyNoInteractions(sessionIssuer);
    }

    private RefreshToken tokenOfAna(Instant issuedAt) {
        return RefreshToken.issue(ana.id(), TOKEN_HASH, "pixel-8", issuedAt, Duration.ofDays(7));
    }
}
