package ec.nexo.customer.application.usecase;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;
import ec.nexo.customer.application.port.out.DeviceTokenRepositoryPort;
import ec.nexo.customer.domain.exception.CustomerNotFoundException;
import ec.nexo.customer.domain.model.DevicePlatform;
import ec.nexo.customer.domain.model.DeviceToken;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** La app registra su token de push al iniciar sesión (y cada vez que FCM lo rota). Es idempotente. */
public class RegisterDeviceUseCase {

    private final CustomerRepositoryPort customers;
    private final DeviceTokenRepositoryPort devices;

    public RegisterDeviceUseCase(CustomerRepositoryPort customers, DeviceTokenRepositoryPort devices) {
        this.customers = customers;
        this.devices = devices;
    }

    @Transactional
    public void execute(UUID customerId, String token, DevicePlatform platform) {
        customers.findById(customerId).orElseThrow(CustomerNotFoundException::new);
        devices.register(new DeviceToken(token, customerId, platform));
    }
}
