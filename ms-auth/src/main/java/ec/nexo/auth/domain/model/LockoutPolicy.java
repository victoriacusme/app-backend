package ec.nexo.auth.domain.model;

/**
 * Regla de bloqueo de credenciales: tras {@code maxFailedAttempts} intentos fallidos consecutivos
 * el usuario queda bloqueado.
 */
public record LockoutPolicy(int maxFailedAttempts) {

    public LockoutPolicy {
        if (maxFailedAttempts < 1) {
            throw new IllegalArgumentException("maxFailedAttempts debe ser al menos 1");
        }
    }

    public boolean shouldLock(int failedAttempts) {
        return failedAttempts >= maxFailedAttempts;
    }
}
