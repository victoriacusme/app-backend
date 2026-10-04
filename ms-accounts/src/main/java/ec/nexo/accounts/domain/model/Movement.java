package ec.nexo.accounts.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Asiento de una cuenta. Es inmutable: un movimiento contabilizado no se edita, se compensa con otro. */
public record Movement(UUID id, UUID accountId, MovementType type, BigDecimal amount, BigDecimal balanceAfter,
                       String description, Instant bookedAt) {

    public Movement {
        Objects.requireNonNull(id);
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(balanceAfter);
        Objects.requireNonNull(description);
        Objects.requireNonNull(bookedAt);
    }
}
