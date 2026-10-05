package ec.nexo.accounts.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransferTest {

    private final UUID source = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();

    @Test
    void laHuellaIgnoraLaFormaDeEscribirElMonto() {
        assertThat(Transfer.fingerprint(source, target, new BigDecimal("10"), "renta"))
                .isEqualTo(Transfer.fingerprint(source, target, new BigDecimal("10.00"), "renta"));
    }

    @Test
    void laHuellaCambiaSiCambiaCualquierDato() {
        String original = Transfer.fingerprint(source, target, new BigDecimal("10.00"), "renta");

        assertThat(Transfer.fingerprint(source, target, new BigDecimal("10.01"), "renta")).isNotEqualTo(original);
        assertThat(Transfer.fingerprint(target, source, new BigDecimal("10.00"), "renta")).isNotEqualTo(original);
        assertThat(Transfer.fingerprint(source, target, new BigDecimal("10.00"), null)).isNotEqualTo(original);
    }
}
