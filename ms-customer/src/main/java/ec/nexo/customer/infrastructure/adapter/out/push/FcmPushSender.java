package ec.nexo.customer.infrastructure.adapter.out.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import ec.nexo.customer.application.port.out.PushSenderPort;
import ec.nexo.customer.domain.model.DeviceToken;
import ec.nexo.customer.domain.model.PushMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Envío real por Firebase Cloud Messaging (API HTTP v1, vía el SDK de administración). */
public class FcmPushSender implements PushSenderPort {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);

    private final FirebaseMessaging messaging;

    public FcmPushSender(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }

    @Override
    public SendResult send(String token, PushMessage message) {
        try {
            messaging.send(Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(message.title())
                            .setBody(message.body())
                            .build())
                    .putAllData(message.data())
                    .build());
            return SendResult.DELIVERED;
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                return SendResult.INVALID_TOKEN;
            }
            log.warn("FCM no pudo entregar a {}: {}", DeviceToken.mask(token), code);
            return SendResult.FAILED;
        }
    }
}
