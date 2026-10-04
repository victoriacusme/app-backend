package ec.nexo.accounts.infrastructure.adapter.out.persistence;

import ec.nexo.accounts.domain.model.AccountStatus;
import ec.nexo.accounts.domain.model.AccountType;
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
@Table(name = "accounts")
class AccountEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 20)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(length = 50)
    private String alias;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AccountEntity() {
    }

    AccountEntity(UUID id, UUID customerId, String number, AccountType type, String currency, BigDecimal balance,
                  AccountStatus status, String alias, boolean isDefault, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.number = number;
        this.type = type;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.alias = alias;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
    }

    void changeBalance(BigDecimal balance) {
        this.balance = balance;
    }

    UUID getId() {
        return id;
    }

    UUID getCustomerId() {
        return customerId;
    }

    String getNumber() {
        return number;
    }

    AccountType getType() {
        return type;
    }

    String getCurrency() {
        return currency;
    }

    BigDecimal getBalance() {
        return balance;
    }

    AccountStatus getStatus() {
        return status;
    }

    String getAlias() {
        return alias;
    }

    boolean isDefault() {
        return isDefault;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
