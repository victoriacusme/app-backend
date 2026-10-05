package ec.nexo.customer.infrastructure.adapter.out.persistence;

import ec.nexo.customer.domain.model.Segment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Solo lectura desde el servicio: la configuración se administra en BD (o, a futuro, desde un backoffice). */
@Entity
@Table(name = "experience_components")
class ExperienceComponentEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 30)
    private String screen;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Segment segment;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false)
    private int position;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> props;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean promotion;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "hour_from")
    private Short hourFrom;

    @Column(name = "hour_to")
    private Short hourTo;

    @Column(name = "customer_id")
    private UUID customerId;

    protected ExperienceComponentEntity() {
    }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getScreen() {
        return screen;
    }

    Segment getSegment() {
        return segment;
    }

    String getType() {
        return type;
    }

    int getPosition() {
        return position;
    }

    Map<String, Object> getProps() {
        return props;
    }

    boolean isActive() {
        return active;
    }

    boolean isPromotion() {
        return promotion;
    }

    Instant getStartsAt() {
        return startsAt;
    }

    Instant getEndsAt() {
        return endsAt;
    }

    Integer getHourFrom() {
        return hourFrom == null ? null : hourFrom.intValue();
    }

    Integer getHourTo() {
        return hourTo == null ? null : hourTo.intValue();
    }

    UUID getCustomerId() {
        return customerId;
    }
}
