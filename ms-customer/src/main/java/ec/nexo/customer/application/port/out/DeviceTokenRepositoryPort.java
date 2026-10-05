package ec.nexo.customer.application.port.out;

import ec.nexo.customer.domain.model.DeviceToken;

import java.util.List;
import java.util.UUID;

public interface DeviceTokenRepositoryPort {

    /** Crea o reasigna el token (si antes era de otro cliente en el mismo teléfono, pasa a este). */
    void register(DeviceToken deviceToken);

    /** Solo borra si el token es de ese cliente: nadie puede desregistrar el teléfono de otro. */
    void deleteByTokenAndCustomerId(String token, UUID customerId);

    void deleteByToken(String token);

    List<DeviceToken> findByCustomerId(UUID customerId);
}
