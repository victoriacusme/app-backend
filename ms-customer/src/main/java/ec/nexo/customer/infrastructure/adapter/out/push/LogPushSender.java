package ec.nexo.customer.infrastructure.adapter.out.push;

import ec.nexo.customer.application.port.out.PushSenderPort;
import ec.nexo.customer.domain.model.DeviceToken;
import ec.nexo.customer.domain.model.PushMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Push simulado para desarrollo y demo: sin credenciales de Firebase, la notificación se registra en el log.
 * El token se enmascara. El texto no lleva datos personales (cuentas enmascaradas).
 */
public class LogPushSender implements PushSenderPort {

    private static final Logger log = LoggerFactory.getLogger(LogPushSender.class);

    @Override
    public SendResult send(String token, PushMessage message) {
        log.info("Push simulado a {}: \"{}\" - \"{}\" {}", DeviceToken.mask(token), message.title(), message.body(),
                message.data());
        return SendResult.DELIVERED;
    }
}
