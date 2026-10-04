package ec.nexo.customer.infrastructure.adapter.out.persistence;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;
import ec.nexo.customer.application.port.out.DuplicateCustomerException;
import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Preferences;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
class CustomerRepositoryAdapter implements CustomerRepositoryPort {

    private final CustomerJpaRepository customers;
    private final PreferencesJpaRepository preferences;

    CustomerRepositoryAdapter(CustomerJpaRepository customers, PreferencesJpaRepository preferences) {
        this.customers = customers;
        this.preferences = preferences;
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return customers.findById(id).map(CustomerRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public void create(Customer customer, Preferences prefs) {
        if (customers.existsById(customer.id())) {
            throw new DuplicateCustomerException(null);
        }
        try {
            customers.saveAndFlush(new CustomerEntity(customer.id(), customer.fullName(), customer.idNumber(),
                    customer.email(), customer.phone(), customer.birthDate(), customer.segment(),
                    customer.createdAt()));
            preferences.saveAndFlush(PreferencesRepositoryAdapter.toEntity(prefs, Instant.now()));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateCustomerException(e);
        }
    }

    private static Customer toDomain(CustomerEntity entity) {
        return new Customer(entity.getId(), entity.getFullName(), entity.getIdNumber(), entity.getEmail(),
                entity.getPhone(), entity.getBirthDate(), entity.getSegment(), entity.getCreatedAt());
    }
}
