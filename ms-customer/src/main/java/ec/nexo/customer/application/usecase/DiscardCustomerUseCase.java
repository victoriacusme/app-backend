package ec.nexo.customer.application.usecase;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;

import java.util.UUID;

/**
 * Compensación del onboarding: ms-auth descarta el cliente que acaba de provisionar cuando no pudo abrir la cuenta.
 * Es idempotente: descartar un cliente que no existe no es un error.
 */
public class DiscardCustomerUseCase {

    private final CustomerRepositoryPort customers;

    public DiscardCustomerUseCase(CustomerRepositoryPort customers) {
        this.customers = customers;
    }

    public void execute(UUID customerId) {
        customers.deleteById(customerId);
    }
}
