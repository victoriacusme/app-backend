package ec.nexo.auth.application.port.out;

import java.util.UUID;

/** Apertura de la cuenta por defecto en ms-accounts durante el onboarding. Idempotente por {@code customerId}. */
public interface AccountProvisioningPort {

    void openDefaultAccount(UUID customerId);
}
