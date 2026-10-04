package ec.nexo.auth.application.usecase;

import ec.nexo.auth.application.port.out.RefreshTokenRepositoryPort;
import ec.nexo.auth.domain.model.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    private static final String RAW_TOKEN = "refresh-en-claro";
    private static final String TOKEN_HASH = RefreshToken.hash(RAW_TOKEN);

    @Mock
    private RefreshTokenRepositoryPort refreshTokens;

    private LogoutUseCase logoutUseCase;

    @BeforeEach
    void setUp() {
        logoutUseCase = new LogoutUseCase(refreshTokens);
    }

    @Test
    void revocaElTokenActivo() {
        RefreshToken token = RefreshToken.issue(UUID.randomUUID(), TOKEN_HASH, null, Instant.now(), Duration.ofDays(7));
        when(refreshTokens.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(token));

        logoutUseCase.execute(RAW_TOKEN);

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokens).save(token);
    }

    @Test
    void esIdempotenteConUnTokenYaRevocado() {
        RefreshToken token = RefreshToken.issue(UUID.randomUUID(), TOKEN_HASH, null, Instant.now(), Duration.ofDays(7));
        token.revoke();
        when(refreshTokens.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(token));

        logoutUseCase.execute(RAW_TOKEN);

        verify(refreshTokens, never()).save(any());
    }

    @Test
    void noFallaConUnTokenDesconocido() {
        when(refreshTokens.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        logoutUseCase.execute(RAW_TOKEN);

        verify(refreshTokens, never()).save(any());
    }
}
