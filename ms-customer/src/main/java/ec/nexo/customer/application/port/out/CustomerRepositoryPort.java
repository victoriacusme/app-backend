package ec.nexo.customer.application.port.out;

import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Preferences;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepositoryPort {

    Optional<Customer> findById(UUID id);

    /**
     * Crea el cliente y sus preferencias en una sola transacción.
     * @throws DuplicateCustomerException si el cliente ya existe (alta simultánea)
     */
    void create(Customer customer, Preferences preferences);

    /** Borra el cliente y sus preferencias. No falla si no existe. */
    void deleteById(UUID id);
}
