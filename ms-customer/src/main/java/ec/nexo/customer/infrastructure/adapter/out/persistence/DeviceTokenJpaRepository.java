package ec.nexo.customer.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface DeviceTokenJpaRepository extends JpaRepository<DeviceTokenEntity, String> {

    List<DeviceTokenEntity> findByCustomerId(UUID customerId);

    @Modifying
    @Query("DELETE FROM DeviceTokenEntity d WHERE d.token = :token AND d.customerId = :customerId")
    void deleteByTokenAndCustomerId(String token, UUID customerId);
}
