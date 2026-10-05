package ec.nexo.customer.application.usecase;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;
import ec.nexo.customer.application.port.out.DuplicateCustomerException;
import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Preferences;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Alta del cliente durante el onboarding (la llama ms-auth). Es idempotente por {@code customerId}:
 * si ms-auth reintenta, se devuelve el cliente ya creado.
 */
public class ProvisionCustomerUseCase {

    private final CustomerRepositoryPort customers;
    private final Clock clock;
    private final ZoneId zone;

    public ProvisionCustomerUseCase(CustomerRepositoryPort customers, Clock clock, ZoneId zone) {
        this.customers = customers;
        this.clock = clock;
        this.zone = zone;
    }

    public Result execute(NewCustomer command) {
        var existing = customers.findById(command.customerId());
        if (existing.isPresent()) {
            return new Result(existing.get(), false);
        }
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Customer customer = Customer.register(command.customerId(), command.fullName(), command.idNumber(),
                command.email(), command.phone(), command.birthDate(), clock.instant(), today);
        try {
            customers.create(customer, Preferences.defaultsFor(customer.id()));
            return new Result(customer, true);
        } catch (DuplicateCustomerException e) {
            return new Result(customers.findById(command.customerId()).orElseThrow(() -> e), false);
        }
    }

    public record NewCustomer(UUID customerId, String fullName, String idNumber, String email, String phone,
                              LocalDate birthDate) {

        /** Nunca imprime datos personales. */
        @Override
        public String toString() {
            return "NewCustomer[customerId=" + customerId + "]";
        }
    }

    public record Result(Customer customer, boolean created) {
    }
}
