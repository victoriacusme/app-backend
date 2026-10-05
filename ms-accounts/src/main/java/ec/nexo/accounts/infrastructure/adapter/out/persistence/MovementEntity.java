package ec.nexo.accounts.infrastructure.adapter.out.persistence;

import ec.nexo.accounts.domain.model.MovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movements")
class MovementEntity implements Persistable<UUID> {

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

    @Column(name = "transfer_id")
    private UUID transferId;

    // Los movimientos solo se insertan: evita el SELECT previo que hace save() con ids asignados.
    @Transient
    private boolean isNew = true;

    protected MovementEntity() {
    }

    MovementEntity(UUID id, UUID accountId, MovementType type, BigDecimal amount, BigDecimal balanceAfter,
                   String description, Instant bookedAt, UUID transferId) {
        this.id = id;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.bookedAt = bookedAt;
        this.transferId = transferId;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public UUID getId() {
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

    UUID getTransferId() {
        return transferId;
    }
}
