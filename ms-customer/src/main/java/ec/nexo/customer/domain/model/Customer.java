package ec.nexo.customer.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Cliente de banca personas. El {@code id} es el {@code sub} del JWT emitido por ms-auth. */
public record Customer(UUID id, String fullName, String idNumber, String email, String phone, LocalDate birthDate,
                       Segment segment, Instant createdAt) {

    private static final int VISIBLE_DIGITS = 4;

    public Customer {
        Objects.requireNonNull(id);
        Objects.requireNonNull(fullName);
        Objects.requireNonNull(idNumber);
        Objects.requireNonNull(email);
        Objects.requireNonNull(phone);
        Objects.requireNonNull(birthDate);
        Objects.requireNonNull(segment);
        Objects.requireNonNull(createdAt);
    }

    public static Customer register(UUID id, String fullName, String idNumber, String email, String phone,
                                    LocalDate birthDate, Instant now, LocalDate today) {
        return new Customer(id, fullName.strip(), idNumber, email.strip().toLowerCase(), phone, birthDate,
                Segment.forNewCustomer(birthDate, today), now);
    }

    public String firstName() {
        String trimmed = fullName.strip();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    public String maskedIdNumber() {
        return mask(idNumber);
    }

    public String maskedPhone() {
        return mask(phone);
    }

    /** Solo datos no sensibles: un cliente puede terminar en un log o en el mensaje de una excepción. */
    @Override
    public String toString() {
        return "Customer[id=" + id + ", segment=" + segment + "]";
    }

    private static String mask(String value) {
        int visibleFrom = Math.max(0, value.length() - VISIBLE_DIGITS);
        return "*".repeat(visibleFrom) + value.substring(visibleFrom);
    }
}
