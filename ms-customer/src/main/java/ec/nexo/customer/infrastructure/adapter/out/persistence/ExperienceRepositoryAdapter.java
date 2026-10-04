package ec.nexo.customer.infrastructure.adapter.out.persistence;

import ec.nexo.customer.application.port.out.ExperienceRepositoryPort;
import ec.nexo.customer.domain.model.ExperienceComponent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ExperienceRepositoryAdapter implements ExperienceRepositoryPort {

    private final ExperienceComponentJpaRepository repository;

    ExperienceRepositoryAdapter(ExperienceComponentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ExperienceComponent> findActiveByScreen(String screen) {
        return repository.findByScreenAndActiveTrueOrderByPositionAsc(screen).stream()
                .map(ExperienceRepositoryAdapter::toDomain)
                .toList();
    }

    private static ExperienceComponent toDomain(ExperienceComponentEntity e) {
        return new ExperienceComponent(e.getId(), e.getName(), e.getScreen(), e.getSegment(), e.getType(),
                e.getPosition(), e.getProps(), e.isActive(), e.isPromotion(), e.getStartsAt(), e.getEndsAt(),
                e.getHourFrom(), e.getHourTo(), e.getCustomerId());
    }
}
