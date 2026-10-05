package ec.nexo.customer.application.port.out;

import ec.nexo.customer.domain.model.Preferences;

import java.util.Optional;
import java.util.UUID;

public interface PreferencesRepositoryPort {

    Optional<Preferences> findByCustomerId(UUID customerId);

    Preferences save(Preferences preferences);
}
