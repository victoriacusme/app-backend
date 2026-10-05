package ec.nexo.customer.domain.model;

import java.time.LocalDate;
import java.time.Period;

/** Segmento del cliente: define qué experiencia ve en la app. */
public enum Segment {
    YOUNG,
    PREMIUM,
    ENTREPRENEUR,
    STANDARD;

    private static final int YOUNG_MAX_AGE = 25;

    /** Un cliente nuevo entra como joven o estándar; PREMIUM y ENTREPRENEUR los asigna el banco después. */
    public static Segment forNewCustomer(LocalDate birthDate, LocalDate today) {
        return Period.between(birthDate, today).getYears() <= YOUNG_MAX_AGE ? YOUNG : STANDARD;
    }
}
