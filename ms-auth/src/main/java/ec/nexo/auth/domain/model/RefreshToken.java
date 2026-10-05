package ec.nexo.auth.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * Refresh token opaco. Solo se persiste su hash SHA-256; el valor en claro únicamente lo conoce el cliente.
 */
public class RefreshToken {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final String deviceId;
    private final Instant expiresAt;
    private final Instant createdAt;
    private boolean revoked;

    private RefreshToken(UUID id, UUID userId, String tokenHash, String deviceId,
                         Instant expiresAt, boolean revoked, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.deviceId = deviceId;
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.revoked = revoked;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static RefreshToken issue(UUID userId, String tokenHash, String deviceId, Instant now, Duration ttl) {
        return new RefreshToken(UUID.randomUUID(), userId, tokenHash, deviceId, now.plus(ttl), false, now);
    }

    public static RefreshToken restore(UUID id, UUID userId, String tokenHash, String deviceId,
                                       Instant expiresAt, boolean revoked, Instant createdAt) {
        return new RefreshToken(id, userId, tokenHash, deviceId, expiresAt, revoked, createdAt);
    }

    /** Genera el valor en claro que se entrega al cliente (256 bits aleatorios, Base64 URL). */
    public static String generateValue() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String rawValue) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void revoke() {
        revoked = true;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public String deviceId() {
        return deviceId;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
