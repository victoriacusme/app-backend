package ec.nexo.accounts.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface TransferJpaRepository extends JpaRepository<TransferEntity, UUID> {

    Optional<TransferEntity> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    Optional<TransferEntity> findByIdAndCustomerId(UUID id, UUID customerId);
}
