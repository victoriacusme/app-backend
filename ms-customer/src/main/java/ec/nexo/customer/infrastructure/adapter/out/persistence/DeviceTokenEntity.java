package ec.nexo.customer.infrastructure.adapter.out.persistence;

import ec.nexo.customer.domain.model.DevicePlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_tokens")
class DeviceTokenEntity {

    @Id
    @Column(length = 512)
    private String token;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeviceTokenEntity() {
    }

    DeviceTokenEntity(String token, UUID customerId, DevicePlatform platform, Instant updatedAt) {
        this.token = token;
        this.customerId = customerId;
        this.platform = platform;
        this.updatedAt = updatedAt;
    }

    String getToken() {
        return token;
    }

    UUID getCustomerId() {
        return customerId;
    }

    DevicePlatform getPlatform() {
        return platform;
    }
}
