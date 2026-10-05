package ec.nexo.auth.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private final LockoutPolicy policy = new LockoutPolicy(5);

    @Test
    void seBloqueaAlQuintoIntentoFallido() {
        User user = User.register("Ana", UUID.randomUUID(), "hash", Instant.now());

        for (int i = 0; i < 4; i++) {
            user.registerFailedAttempt(policy);
        }
        assertThat(user.isLocked()).isFalse();

        user.registerFailedAttempt(policy);
        assertThat(user.isLocked()).isTrue();
        assertThat(user.failedAttempts()).isEqualTo(5);
    }

    @Test
    void unLoginCorrectoReiniciaElContador() {
        User user = User.register("ana", UUID.randomUUID(), "hash", Instant.now());
        user.registerFailedAttempt(policy);
        user.registerFailedAttempt(policy);

        user.registerSuccessfulLogin();

        assertThat(user.failedAttempts()).isZero();
    }

    @Test
    void normalizaElUsername() {
        assertThat(User.normalizeUsername("  Carlos ")).isEqualTo("carlos");
    }
}
