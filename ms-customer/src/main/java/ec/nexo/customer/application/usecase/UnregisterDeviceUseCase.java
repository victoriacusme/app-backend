package ec.nexo.customer.application.usecase;

import ec.nexo.customer.application.port.out.DeviceTokenRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Al cerrar sesión la app borra su token: ese teléfono deja de recibir avisos de este cliente. Idempotente. */
public class UnregisterDeviceUseCase {

    private final DeviceTokenRepositoryPort devices;

    public UnregisterDeviceUseCase(DeviceTokenRepositoryPort devices) {
        this.devices = devices;
    }

    @Transactional
    public void execute(UUID customerId, String token) {
        devices.deleteByTokenAndCustomerId(token, customerId);
    }
}
