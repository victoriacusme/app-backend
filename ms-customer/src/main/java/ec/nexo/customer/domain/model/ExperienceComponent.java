package ec.nexo.customer.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Un bloque de una pantalla server-driven, tal como está configurado en BD.
 * {@code segment} nulo aplica a todos los segmentos; {@code customerId} lo restringe a un cliente concreto
 * (para componentes con datos propios, como una meta de ahorro). Fechas y franja horaria son opcionales.
 */
public record ExperienceComponent(UUID id, String name, String screen, Segment segment, String type, int position,
                                  Map<String, Object> props, boolean active, boolean promotion, Instant startsAt,
                                  Instant endsAt, Integer hourFrom, Integer hourTo, UUID customerId) {

    public ExperienceComponent {
        Objects.requireNonNull(id);
        Objects.requireNonNull(screen);
        Objects.requireNonNull(type);
        props = props == null ? Map.of() : props;
    }

    /** ¿Se muestra este componente a este cliente, en este momento y con estas preferencias? */
    public boolean isVisibleTo(Customer customer, Preferences preferences, Instant now, int localHour) {
        return active
                && (segment == null || segment == customer.segment())
                && (customerId == null || customerId.equals(customer.id()))
                && (startsAt == null || !now.isBefore(startsAt))
                && (endsAt == null || now.isBefore(endsAt))
                && (hourFrom == null || hourTo == null || (localHour >= hourFrom && localHour < hourTo))
                && (!promotion || preferences.showPromotions());
    }
}
