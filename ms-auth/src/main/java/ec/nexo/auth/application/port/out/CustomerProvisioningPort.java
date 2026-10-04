package ec.nexo.auth.application.port.out;

import java.time.LocalDate;
import java.util.UUID;

/** Alta del cliente en ms-customer durante el onboarding. Debe ser idempotente por {@code customerId}. */
public interface CustomerProvisioningPort {

    void provision(NewCustomer customer);

    record NewCustomer(UUID customerId, String fullName, String idNumber, String email,
                       String phone, LocalDate birthDate) {
    }
}
