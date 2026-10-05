package ec.nexo.customer.application.usecase;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;
import ec.nexo.customer.application.port.out.PreferencesRepositoryPort;
import ec.nexo.customer.domain.exception.CustomerNotFoundException;
import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Preferences;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class GetMyProfileUseCase {

    private final CustomerRepositoryPort customers;
    private final PreferencesRepositoryPort preferences;

    public GetMyProfileUseCase(CustomerRepositoryPort customers, PreferencesRepositoryPort preferences) {
        this.customers = customers;
        this.preferences = preferences;
    }

    @Transactional(readOnly = true)
    public CustomerProfile execute(UUID customerId) {
        Customer customer = customers.findById(customerId).orElseThrow(CustomerNotFoundException::new);
        Preferences prefs = preferences.findByCustomerId(customerId)
                .orElseGet(() -> Preferences.defaultsFor(customerId));
        return new CustomerProfile(customer, prefs);
    }
}
