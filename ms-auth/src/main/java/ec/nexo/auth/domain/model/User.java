package ec.nexo.auth.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Credenciales de un cliente. El {@code customerId} es el identificador que viaja en el JWT ({@code sub})
 * y que el resto de microservicios usa para resolver al cliente.
 */
public class User {

    private final UUID id;
    private final String username;
    private final UUID customerId;
    private final String passwordHash;
    private final Instant createdAt;
    private UserStatus status;
    private int failedAttempts;

    private User(UUID id, String username, UUID customerId, String passwordHash,
                 UserStatus status, int failedAttempts, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.username = Objects.requireNonNull(username);
        this.customerId = Objects.requireNonNull(customerId);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.status = Objects.requireNonNull(status);
        this.failedAttempts = failedAttempts;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static User register(String username, UUID customerId, String passwordHash, Instant now) {
        return new User(UUID.randomUUID(), normalizeUsername(username), customerId, passwordHash,
                UserStatus.ACTIVE, 0, now);
    }

    public static User restore(UUID id, String username, UUID customerId, String passwordHash,
                               UserStatus status, int failedAttempts, Instant createdAt) {
        return new User(id, username, customerId, passwordHash, status, failedAttempts, createdAt);
    }

    public static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public void registerFailedAttempt(LockoutPolicy policy) {
        failedAttempts++;
        if (policy.shouldLock(failedAttempts)) {
            status = UserStatus.LOCKED;
        }
    }

    public void registerSuccessfulLogin() {
        failedAttempts = 0;
    }

    public boolean isLocked() {
        return status == UserStatus.LOCKED;
    }

    public UUID id() {
        return id;
    }

    public String username() {
        return username;
    }

    public UUID customerId() {
        return customerId;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public UserStatus status() {
        return status;
    }

    public int failedAttempts() {
        return failedAttempts;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
