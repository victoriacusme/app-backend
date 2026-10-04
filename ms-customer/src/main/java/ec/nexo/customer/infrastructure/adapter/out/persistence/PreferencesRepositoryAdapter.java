package ec.nexo.customer.infrastructure.adapter.out.persistence;

import ec.nexo.customer.application.port.out.PreferencesRepositoryPort;
import ec.nexo.customer.domain.model.Preferences;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
class PreferencesRepositoryAdapter implements PreferencesRepositoryPort {

    private final PreferencesJpaRepository repository;

    PreferencesRepositoryAdapter(PreferencesJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Preferences> findByCustomerId(UUID customerId) {
        return repository.findById(customerId).map(PreferencesRepositoryAdapter::toDomain);
    }

    @Override
    public Preferences save(Preferences preferences) {
        return toDomain(repository.save(toEntity(preferences, Instant.now())));
    }

    static PreferencesEntity toEntity(Preferences preferences, Instant updatedAt) {
        return new PreferencesEntity(preferences.customerId(), preferences.language(), preferences.theme(),
                preferences.notificationsEnabled(), preferences.showPromotions(), updatedAt);
    }

    private static Preferences toDomain(PreferencesEntity entity) {
        return Preferences.restore(entity.getCustomerId(), entity.getLanguage(), entity.getTheme(),
                entity.isNotificationsEnabled(), entity.isShowPromotions());
    }
}
