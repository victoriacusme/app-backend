package ec.nexo.accounts.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
}
