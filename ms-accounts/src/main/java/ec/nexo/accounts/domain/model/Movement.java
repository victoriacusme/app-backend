package ec.nexo.accounts.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Asiento de una cuenta. Es inmutable: un movimiento contabilizado no se edita, se compensa con otro. */
public record Movement(UUID id, UUID accountId, MovementType type, BigDecimal amount, BigDecimal balanceAfter,
                       String description, Instant bookedAt, UUID transferId) {

    private static final int MAX_DESCRIPTION = 140;

    public Movement {
        Objects.requireNonNull(id);
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(balanceAfter);
        Objects.requireNonNull(description);
        Objects.requireNonNull(bookedAt);
    }

    /** Débito en la cuenta origen; se registra con el saldo ya actualizado. */
    public static Movement debitOf(Transfer transfer, Account source, Account target) {
        return new Movement(UUID.randomUUID(), source.id(), MovementType.DEBIT, transfer.amount(), source.balance(),
                describe("Transferencia a " + target.maskedNumber(), transfer.description()), transfer.createdAt(),
                transfer.id());
    }

    /** Crédito en la cuenta destino; se registra con el saldo ya actualizado. */
    public static Movement creditOf(Transfer transfer, Account source, Account target) {
        return new Movement(UUID.randomUUID(), target.id(), MovementType.CREDIT, transfer.amount(), target.balance(),
                describe("Transferencia desde " + source.maskedNumber(), transfer.description()),
                transfer.createdAt(), transfer.id());
    }

    private static String describe(String base, String userDescription) {
        String text = userDescription == null || userDescription.isBlank() ? base : base + " - " + userDescription;
        return text.length() <= MAX_DESCRIPTION ? text : text.substring(0, MAX_DESCRIPTION);
    }
}
