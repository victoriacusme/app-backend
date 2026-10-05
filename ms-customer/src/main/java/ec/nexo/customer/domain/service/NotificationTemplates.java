package ec.nexo.customer.domain.service;

import ec.nexo.customer.domain.exception.InvalidNotificationException;
import ec.nexo.customer.domain.model.NotificationType;
import ec.nexo.customer.domain.model.PushMessage;

import java.util.Map;

/**
 * Textos de las notificaciones en el idioma del cliente. Los servicios que notifican (p. ej. ms-accounts) solo
 * envían el evento y sus datos; el texto se arma aquí porque aquí viven las preferencias del cliente.
 */
public class NotificationTemplates {

    public PushMessage render(NotificationType type, Map<String, String> data, String language) {
        boolean english = "en".equals(language);
        return switch (type) {
            case TRANSFER_COMPLETED -> {
                String transferId = required(data, "transferId");
                String amount = money(required(data, "amount"), required(data, "currency"));
                String target = required(data, "targetAccount");
                yield new PushMessage(
                        english ? "Transfer completed" : "Transferencia exitosa",
                        english ? "You transferred " + amount + " to your account " + target
                                : "Transferiste " + amount + " a tu cuenta " + target,
                        Map.of("type", type.name(),
                                "transferId", transferId,
                                "deeplink", "app://transfers/" + transferId));
            }
        };
    }

    private static String money(String amount, String currency) {
        return "USD".equals(currency) ? "$" + amount : amount + " " + currency;
    }

    private static String required(Map<String, String> data, String key) {
        String value = data == null ? null : data.get(key);
        if (value == null || value.isBlank()) {
            throw new InvalidNotificationException("Falta el dato '" + key + "' de la notificación");
        }
        return value;
    }
}
