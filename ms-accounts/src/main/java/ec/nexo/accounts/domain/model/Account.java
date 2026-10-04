package ec.nexo.accounts.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Cuenta de un cliente. El {@code customerId} es el {@code sub} del JWT emitido por ms-auth. */
public class Account {

    public static final String DEFAULT_CURRENCY = "USD";
    private static final int VISIBLE_DIGITS = 4;

    private final UUID id;
    private final UUID customerId;
    private final String number;
    private final AccountType type;
    private final String currency;
    private final String alias;
    private final boolean isDefault;
    private final Instant createdAt;
    private BigDecimal balance;
    private AccountStatus status;

    private Account(UUID id, UUID customerId, String number, AccountType type, String currency, BigDecimal balance,
                    AccountStatus status, String alias, boolean isDefault, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.customerId = Objects.requireNonNull(customerId);
        this.number = Objects.requireNonNull(number);
        this.type = Objects.requireNonNull(type);
        this.currency = Objects.requireNonNull(currency);
        this.balance = Objects.requireNonNull(balance);
        this.status = Objects.requireNonNull(status);
        this.alias = alias;
        this.isDefault = isDefault;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /** Cuenta de ahorros con saldo cero que se abre durante el onboarding. */
    public static Account openDefault(UUID customerId, String number, Instant now) {
        return new Account(UUID.randomUUID(), customerId, number, AccountType.SAVINGS, DEFAULT_CURRENCY,
                BigDecimal.ZERO.setScale(2), AccountStatus.ACTIVE, "Cuenta de ahorros", true, now);
    }

    public static Account restore(UUID id, UUID customerId, String number, AccountType type, String currency,
                                  BigDecimal balance, AccountStatus status, String alias, boolean isDefault,
                                  Instant createdAt) {
        return new Account(id, customerId, number, type, currency, balance, status, alias, isDefault, createdAt);
    }

    /** Número enmascarado para exponer fuera del servicio, p. ej. {@code ****4521}. */
    public String maskedNumber() {
        return "****" + number.substring(Math.max(0, number.length() - VISIBLE_DIGITS));
    }

    public UUID id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public String number() {
        return number;
    }

    public AccountType type() {
        return type;
    }

    public String currency() {
        return currency;
    }

    public BigDecimal balance() {
        return balance;
    }

    public AccountStatus status() {
        return status;
    }

    public String alias() {
        return alias;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
