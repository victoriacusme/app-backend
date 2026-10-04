package ec.nexo.customer.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ExperienceComponentJpaRepository extends JpaRepository<ExperienceComponentEntity, UUID> {

    List<ExperienceComponentEntity> findByScreenAndActiveTrueOrderByPositionAsc(String screen);
}
