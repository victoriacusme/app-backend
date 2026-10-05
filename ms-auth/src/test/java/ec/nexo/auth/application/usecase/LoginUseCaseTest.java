package ec.nexo.auth.application.usecase;

import ec.nexo.auth.application.port.out.PasswordHasherPort;
import ec.nexo.auth.application.port.out.UserRepositoryPort;
import ec.nexo.auth.application.usecase.LoginUseCase.LoginCommand;
import ec.nexo.auth.domain.exception.InvalidCredentialsException;
import ec.nexo.auth.domain.exception.UserLockedException;
import ec.nexo.auth.domain.model.AuthTokens;
import ec.nexo.auth.domain.model.LockoutPolicy;
import ec.nexo.auth.domain.model.User;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    private static final String PASSWORD_HASH = "hash-ana";

    @Mock
    private UserRepositoryPort users;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private SessionIssuer sessionIssuer;

    private LoginUseCase loginUseCase;
    private User ana;

    @BeforeEach
    void setUp() {
        loginUseCase = new LoginUseCase(users, passwordHasher, sessionIssuer, new LockoutPolicy(5));
        ana = User.register("ana", UUID.randomUUID(), PASSWORD_HASH, Instant.parse("2026-10-03T12:00:00Z"));
    }

    @Test
    void loginCorrectoNormalizaElUsernameYAbreSesion() {
        AuthTokens tokens = new AuthTokens("access", Duration.ofMinutes(15), "refresh", Duration.ofDays(7));
        when(users.findByUsernameForUpdate("ana")).thenReturn(Optional.of(ana));
        when(passwordHasher.matches("Nexo2026*", PASSWORD_HASH)).thenReturn(true);
        when(sessionIssuer.open(ana, "pixel-8")).thenReturn(tokens);

        AuthTokens result = loginUseCase.execute(new LoginCommand(" ANA ", "Nexo2026*", "pixel-8"));

        assertThat(result).isSameAs(tokens);
        verify(users, never()).save(any());
    }

    @Test
    void usuarioInexistenteVerificaContraUnHashFicticioYDaCredencialesInvalidas() {
        when(users.findByUsernameForUpdate("nadie")).thenReturn(Optional.empty());
        when(passwordHasher.hash(anyString())).thenReturn("dummy-hash");

        assertThatThrownBy(() -> loginUseCase.execute(new LoginCommand("nadie", "x", null)))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordHasher).matches("x", "dummy-hash");
        verifyNoInteractions(sessionIssuer);
    }

    @Test
    void contrasenaIncorrectaRegistraElIntentoFallido() {
        when(users.findByUsernameForUpdate("ana")).thenReturn(Optional.of(ana));
        when(passwordHasher.matches("mala", PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> loginUseCase.execute(new LoginCommand("ana", "mala", null)))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(ana.failedAttempts()).isEqualTo(1);
        verify(users).save(ana);
        verifyNoInteractions(sessionIssuer);
    }

    @Test
    void elQuintoIntentoFallidoBloqueaAlUsuario() {
        when(users.findByUsernameForUpdate("ana")).thenReturn(Optional.of(ana));
        when(passwordHasher.matches("mala", PASSWORD_HASH)).thenReturn(false);

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> loginUseCase.execute(new LoginCommand("ana", "mala", null)))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
        assertThatThrownBy(() -> loginUseCase.execute(new LoginCommand("ana", "mala", null)))
                .isInstanceOf(UserLockedException.class);

        assertThat(ana.isLocked()).isTrue();
        verify(users, times(5)).save(ana);
    }

    @Test
    void usuarioBloqueadoEsRechazadoSinVerificarLaContrasena() {
        for (int i = 0; i < 5; i++) {
            ana.registerFailedAttempt(new LockoutPolicy(5));
        }
        when(users.findByUsernameForUpdate("ana")).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> loginUseCase.execute(new LoginCommand("ana", "Nexo2026*", null)))
                .isInstanceOf(UserLockedException.class);

        verify(passwordHasher, never()).matches(anyString(), anyString());
        verifyNoInteractions(sessionIssuer);
    }

    @Test
    void loginCorrectoReiniciaLosIntentosFallidos() {
        ana.registerFailedAttempt(new LockoutPolicy(5));
        when(users.findByUsernameForUpdate("ana")).thenReturn(Optional.of(ana));
        when(passwordHasher.matches("Nexo2026*", PASSWORD_HASH)).thenReturn(true);

        loginUseCase.execute(new LoginCommand("ana", "Nexo2026*", null));

        assertThat(ana.failedAttempts()).isZero();
        verify(users).save(ana);
        verify(sessionIssuer).open(ana, null);
    }
}
