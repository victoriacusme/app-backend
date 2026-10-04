package ec.nexo.customer.application.port.out;

import ec.nexo.customer.domain.model.PushMessage;

/** Envío de una notificación a un dispositivo (FCM en producción; log en desarrollo). */
public interface PushSenderPort {

    SendResult send(String token, PushMessage message);

    enum SendResult {
        DELIVERED,
        /** El token ya no existe (app desinstalada o token rotado): hay que borrarlo. */
        INVALID_TOKEN,
        FAILED
    }
}
