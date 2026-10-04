package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.domain.model.Account;
import ec.nexo.accounts.domain.model.AccountStatus;
import ec.nexo.accounts.domain.model.AccountType;
import ec.nexo.accounts.domain.model.Movement;
import ec.nexo.accounts.domain.model.MovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Datos de prueba de dominio (no son dobles: los colaboradores se simulan con Mockito). */
final class Fixtures {

    static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");

    private Fixtures() {
    }

    static Account account(UUID customerId) {
        return Account.restore(UUID.randomUUID(), customerId, "2200014521", AccountType.SAVINGS, "USD",
                new BigDecimal("1250.50"), AccountStatus.ACTIVE, "Ahorros", true, NOW);
    }

    static Account account(UUID customerId, String number, String balance) {
        return Account.restore(UUID.randomUUID(), customerId, number, AccountType.SAVINGS, "USD",
                new BigDecimal(balance), AccountStatus.ACTIVE, null, false, NOW);
    }

    static Account blockedAccount(UUID customerId, String balance) {
        return Account.restore(UUID.randomUUID(), customerId, "2200099999", AccountType.SAVINGS, "USD",
                new BigDecimal(balance), AccountStatus.BLOCKED, null, false, NOW);
    }

    static Movement movement(UUID accountId, int minutesAgo) {
        return new Movement(UUID.randomUUID(), accountId, MovementType.DEBIT, new BigDecimal("10.00"),
                new BigDecimal("100.00"), "Compra", NOW.minusSeconds(minutesAgo * 60L), null);
    }
}
