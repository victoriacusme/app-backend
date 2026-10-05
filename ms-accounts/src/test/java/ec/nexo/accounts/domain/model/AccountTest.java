package ec.nexo.accounts.domain.model;

import ec.nexo.accounts.domain.exception.AccountNotActiveException;
import ec.nexo.accounts.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void enmascaraElNumeroDejandoLosUltimosCuatroDigitos() {
        Account account = Account.openDefault(UUID.randomUUID(), "2200014521", Instant.now());

        assertThat(account.maskedNumber()).isEqualTo("****4521");
    }

    @Test
    void laCuentaPorDefectoEsDeAhorrosActivaYConSaldoCero() {
        UUID customerId = UUID.randomUUID();

        Account account = Account.openDefault(customerId, "2200100000", Instant.now());

        assertThat(account.customerId()).isEqualTo(customerId);
        assertThat(account.type()).isEqualTo(AccountType.SAVINGS);
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.balance()).isEqualByComparingTo("0.00");
        assertThat(account.currency()).isEqualTo("USD");
        assertThat(account.isDefault()).isTrue();
    }

    @Test
    void debitaYAcreditaElSaldo() {
        Account account = withBalance("100.00", AccountStatus.ACTIVE);

        account.debit(new BigDecimal("30.25"));
        account.credit(new BigDecimal("5.00"));

        assertThat(account.balance()).isEqualByComparingTo("74.75");
    }

    @Test
    void permiteDebitarExactamenteTodoElSaldo() {
        Account account = withBalance("10.00", AccountStatus.ACTIVE);

        account.debit(new BigDecimal("10.00"));

        assertThat(account.balance()).isEqualByComparingTo("0");
    }

    @Test
    void noPermiteDejarElSaldoNegativo() {
        Account account = withBalance("10.00", AccountStatus.ACTIVE);

        assertThatThrownBy(() -> account.debit(new BigDecimal("10.01"))).isInstanceOf(InsufficientFundsException.class);
        assertThat(account.balance()).isEqualByComparingTo("10.00");
    }

    @Test
    void unaCuentaBloqueadaNoSeMueve() {
        Account account = withBalance("100.00", AccountStatus.BLOCKED);

        assertThatThrownBy(() -> account.debit(BigDecimal.ONE)).isInstanceOf(AccountNotActiveException.class);
        assertThatThrownBy(() -> account.credit(BigDecimal.ONE)).isInstanceOf(AccountNotActiveException.class);
    }

    private static Account withBalance(String balance, AccountStatus status) {
        return Account.restore(UUID.randomUUID(), UUID.randomUUID(), "2200014521", AccountType.SAVINGS, "USD",
                new BigDecimal(balance), status, null, false, Instant.now());
    }
}
