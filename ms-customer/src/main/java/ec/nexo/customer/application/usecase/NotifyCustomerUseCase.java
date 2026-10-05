package ec.nexo.customer.application.usecase;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;
import ec.nexo.customer.application.port.out.DeviceTokenRepositoryPort;
import ec.nexo.customer.application.port.out.PreferencesRepositoryPort;
import ec.nexo.customer.application.port.out.PushSenderPort;
import ec.nexo.customer.application.port.out.PushSenderPort.SendResult;
import ec.nexo.customer.domain.exception.CustomerNotFoundException;
import ec.nexo.customer.domain.model.DeviceToken;
import ec.nexo.customer.domain.model.NotificationType;
import ec.nexo.customer.domain.model.Preferences;
import ec.nexo.customer.domain.model.PushMessage;
import ec.nexo.customer.domain.service.NotificationTemplates;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Notifica un evento a todos los dispositivos del cliente, respetando su preferencia de notificaciones y su idioma.
 * Los tokens que FCM reporta como inválidos se borran para no reintentar con ellos.
 */
public class NotifyCustomerUseCase {

    private final CustomerRepositoryPort customers;
    private final PreferencesRepositoryPort preferences;
    private final DeviceTokenRepositoryPort devices;
    private final PushSenderPort pushSender;
    private final NotificationTemplates templates;

    public NotifyCustomerUseCase(CustomerRepositoryPort customers, PreferencesRepositoryPort preferences,
                                 DeviceTokenRepositoryPort devices, PushSenderPort pushSender,
                                 NotificationTemplates templates) {
        this.customers = customers;
        this.preferences = preferences;
        this.devices = devices;
        this.pushSender = pushSender;
        this.templates = templates;
    }

    public Result execute(UUID customerId, NotificationType type, Map<String, String> data) {
        customers.findById(customerId).orElseThrow(CustomerNotFoundException::new);
        Preferences prefs = preferences.findByCustomerId(customerId)
                .orElseGet(() -> Preferences.defaultsFor(customerId));
        // Se valida el evento antes de mirar la preferencia: un evento mal formado es un error del emisor.
        PushMessage message = templates.render(type, data, prefs.language());
        if (!prefs.notificationsEnabled()) {
            return Result.skipped(Skipped.NOTIFICATIONS_DISABLED);
        }
        List<DeviceToken> targets = devices.findByCustomerId(customerId);
        if (targets.isEmpty()) {
            return Result.skipped(Skipped.NO_DEVICES);
        }

        int delivered = 0;
        int removed = 0;
        int failed = 0;
        for (DeviceToken device : targets) {
            SendResult result = pushSender.send(device.token(), message);
            switch (result) {
                case DELIVERED -> delivered++;
                case INVALID_TOKEN -> {
                    devices.deleteByToken(device.token());
                    removed++;
                }
                case FAILED -> failed++;
            }
        }
        return new Result(delivered, removed, failed, null);
    }

    public enum Skipped {
        NOTIFICATIONS_DISABLED,
        NO_DEVICES
    }

    public record Result(int delivered, int invalidTokensRemoved, int failed, Skipped skipped) {

        static Result skipped(Skipped reason) {
            return new Result(0, 0, 0, reason);
        }
    }
}
