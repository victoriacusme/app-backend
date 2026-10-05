package ec.nexo.accounts.domain.model;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/** Transferencia entre dos cuentas del mismo cliente. Inmutable una vez completada. */
public record Transfer(UUID id, UUID customerId, String idempotencyKey, String requestHash, UUID sourceAccountId,
                       UUID targetAccountId, BigDecimal amount, String currency, String description,
                       TransferStatus status, Instant createdAt) {

    public Transfer {
        Objects.requireNonNull(id);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(idempotencyKey);
        Objects.requireNonNull(requestHash);
        Objects.requireNonNull(sourceAccountId);
        Objects.requireNonNull(targetAccountId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
    }

    public static Transfer completed(UUID customerId, String idempotencyKey, String requestHash, Account source,
                                     Account target, BigDecimal amount, String description, Instant now) {
        return new Transfer(UUID.randomUUID(), customerId, idempotencyKey, requestHash, source.id(), target.id(),
                amount, source.currency(), description, TransferStatus.COMPLETED, now);
    }

    /**
     * Huella del contenido de la solicitud. Normaliza el monto ("10" y "10.00" son lo mismo) para que un reintento
     * legítimo con la misma Idempotency-Key no se confunda con un uso indebido de la clave.
     */
    public static String fingerprint(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount,
                                     String description) {
        String canonical = sourceAccountId + "|" + targetAccountId + "|"
                + amount.stripTrailingZeros().toPlainString() + "|" + (description == null ? "" : description);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
