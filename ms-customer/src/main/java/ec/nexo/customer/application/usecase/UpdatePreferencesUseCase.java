package ec.nexo.customer.application.usecase;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;
import ec.nexo.customer.application.port.out.PreferencesRepositoryPort;
import ec.nexo.customer.domain.exception.CustomerNotFoundException;
import ec.nexo.customer.domain.model.Preferences;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class UpdatePreferencesUseCase {

    private final CustomerRepositoryPort customers;
    private final PreferencesRepositoryPort preferences;

    public UpdatePreferencesUseCase(CustomerRepositoryPort customers, PreferencesRepositoryPort preferences) {
        this.customers = customers;
        this.preferences = preferences;
    }

    @Transactional
    public Preferences execute(UUID customerId, Preferences.Change change) {
        customers.findById(customerId).orElseThrow(CustomerNotFoundException::new);
        Preferences current = preferences.findByCustomerId(customerId)
                .orElseGet(() -> Preferences.defaultsFor(customerId));
        current.apply(change);
        return preferences.save(current);
    }
}
