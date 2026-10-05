package ec.nexo.accounts.infrastructure.adapter.out.notification;

import ec.nexo.accounts.application.port.out.NotificationPort;
import ec.nexo.accounts.application.port.out.TransferNotification;
import ec.nexo.accounts.infrastructure.adapter.in.web.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Pide a ms-customer que notifique al cliente (ms-customer conoce sus dispositivos, idioma y preferencias).
 * Se envía después del commit y en segundo plano: la transferencia no espera al aviso, un rollback no avisa
 * y un fallo al avisar nunca deshace la transferencia. El log no incluye números de cuenta ni datos personales.
 */
public class CustomerNotificationAdapter implements NotificationPort {

    static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final Logger log = LoggerFactory.getLogger(CustomerNotificationAdapter.class);

    private final RestClient client;
    private final Executor executor;

    public CustomerNotificationAdapter(RestClient client, Executor executor) {
        this.client = client;
        this.executor = executor;
    }

    @Override
    public void transferCompleted(TransferNotification notification) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        Runnable send = () -> {
            if (context != null) {
                MDC.setContextMap(context);
            }
            try {
                post(notification);
            } finally {
                MDC.clear();
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(send);
                }
            });
        } else {
            executor.execute(send);
        }
    }

    private void post(TransferNotification notification) {
        try {
            client.post()
                    .uri("/internal/notifications")
                    .headers(headers -> {
                        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
                        if (correlationId != null) {
                            headers.set(CorrelationIdFilter.HEADER, correlationId);
                        }
                    })
                    .body(new NotificationRequest(notification.customerId(), "TRANSFER_COMPLETED", Map.of(
                            "transferId", notification.transferId().toString(),
                            "amount", notification.amount().toPlainString(),
                            "currency", notification.currency(),
                            "targetAccount", notification.targetAccount())))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Notificación de la transferencia {} enviada a ms-customer", notification.transferId());
        } catch (RestClientException e) {
            log.warn("No se pudo notificar la transferencia {}: {}", notification.transferId(), e.getMessage());
        }
    }

    record NotificationRequest(java.util.UUID customerId, String type, Map<String, String> data) {
    }
}
