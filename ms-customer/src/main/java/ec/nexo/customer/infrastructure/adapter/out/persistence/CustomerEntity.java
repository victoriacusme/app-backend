package ec.nexo.customer.infrastructure.adapter.out.persistence;

import ec.nexo.customer.domain.model.Segment;
import ec.nexo.customer.infrastructure.adapter.out.persistence.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customers")
class CustomerEntity {

    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    // Datos personales cifrados en reposo: en la BD solo hay "v1:<base64>".
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "id_number", nullable = false)
    private String idNumber;

    @Column(nullable = false, length = 120)
    private String email;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false)
    private String phone;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Segment segment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CustomerEntity() {
    }

    CustomerEntity(UUID id, String fullName, String idNumber, String email, String phone, LocalDate birthDate,
                   Segment segment, Instant createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.idNumber = idNumber;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
        this.segment = segment;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    String getFullName() {
        return fullName;
    }

    String getIdNumber() {
        return idNumber;
    }

    String getEmail() {
        return email;
    }

    String getPhone() {
        return phone;
    }

    LocalDate getBirthDate() {
        return birthDate;
    }

    Segment getSegment() {
        return segment;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
