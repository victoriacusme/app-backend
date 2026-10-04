package ec.nexo.accounts.infrastructure.adapter.out.persistence;

import ec.nexo.accounts.domain.model.MovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movements")
class MovementEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MovementType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 140)
    private String description;

    @Column(name = "booked_at", nullable = false)
    private Instant bookedAt;

    protected MovementEntity() {
    }

    UUID getId() {
        return id;
    }

    UUID getAccountId() {
        return accountId;
    }

    MovementType getType() {
        return type;
    }

    BigDecimal getAmount() {
        return amount;
    }

    BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    String getDescription() {
        return description;
    }

    Instant getBookedAt() {
        return bookedAt;
    }
}
