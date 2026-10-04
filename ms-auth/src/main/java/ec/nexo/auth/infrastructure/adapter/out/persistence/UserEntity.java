package ec.nexo.auth.infrastructure.adapter.out.persistence;

import ec.nexo.auth.domain.model.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserEntity() {
    }

    UserEntity(UUID id, String username, UUID customerId, String passwordHash,
               UserStatus status, int failedAttempts, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.customerId = customerId;
        this.passwordHash = passwordHash;
        this.status = status;
        this.failedAttempts = failedAttempts;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    String getUsername() {
        return username;
    }

    UUID getCustomerId() {
        return customerId;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    UserStatus getStatus() {
        return status;
    }

    int getFailedAttempts() {
        return failedAttempts;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
