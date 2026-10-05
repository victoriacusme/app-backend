package ec.nexo.customer.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PreferencesJpaRepository extends JpaRepository<PreferencesEntity, UUID> {
}
