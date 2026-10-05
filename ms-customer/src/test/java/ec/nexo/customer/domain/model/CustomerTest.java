package ec.nexo.customer.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);

    @Test
    void enmascaraCedulaYTelefono() {
        Customer customer = register("Ana Torres", LocalDate.of(2003, 5, 14));

        assertThat(customer.maskedIdNumber()).isEqualTo("******5678");
        assertThat(customer.maskedPhone()).isEqualTo("*********4567");
    }

    @Test
    void elPrimerNombreEsLaPrimeraPalabra() {
        assertThat(register("  Lucía Paredes Vera ", LocalDate.of(1990, 1, 1)).firstName()).isEqualTo("Lucía");
        assertThat(register("Cher", LocalDate.of(1990, 1, 1)).firstName()).isEqualTo("Cher");
    }

    @Test
    void unClienteNuevoHastaLos25EsJovenYDespuesEstandar() {
        assertThat(register("A", TODAY.minusYears(25)).segment()).isEqualTo(Segment.YOUNG);
        assertThat(register("A", TODAY.minusYears(26)).segment()).isEqualTo(Segment.STANDARD);
    }

    @Test
    void suToStringNoExponeDatosPersonales() {
        String text = register("Ana Torres", LocalDate.of(2003, 5, 14)).toString();

        assertThat(text).doesNotContain("1712345678", "+593991234567", "ana@nexo.ec", "Ana Torres");
    }

    @Test
    void normalizaElEmail() {
        assertThat(register("A", TODAY.minusYears(30)).email()).isEqualTo("ana@nexo.ec");
    }

    private static Customer register(String fullName, LocalDate birthDate) {
        return Customer.register(UUID.randomUUID(), fullName, "1712345678", "  Ana@Nexo.EC ", "+593991234567",
                birthDate, Instant.now(), TODAY);
    }
}
