package ec.nexo.auth.application.port.out;

import java.time.LocalDate;
import java.util.UUID;

/** Alta del cliente en ms-customer durante el onboarding. Debe ser idempotente por {@code customerId}. */
public interface CustomerProvisioningPort {

    void provision(NewCustomer customer);

    /** Compensación: descarta un cliente recién provisionado cuando el onboarding no pudo completarse. */
    void discard(UUID customerId);

    record NewCustomer(UUID customerId, String fullName, String idNumber, String email,
                       String phone, LocalDate birthDate) {

        /** Nunca imprime datos personales. */
        @Override
        public String toString() {
            return "NewCustomer[customerId=" + customerId + "]";
        }
    }
}
