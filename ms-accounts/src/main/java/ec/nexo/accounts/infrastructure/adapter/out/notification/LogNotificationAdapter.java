package ec.nexo.accounts.infrastructure.adapter.out.notification;

import ec.nexo.accounts.application.port.out.NotificationPort;
import ec.nexo.accounts.domain.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Notificación de desarrollo: la registra en el log. En la Fase 6 se agrega el adaptador FCM.
 * Se envía después del commit: si la transacción se revierte no hay aviso, y un fallo al avisar
 * nunca deshace la transferencia. El log no incluye números de cuenta ni datos personales.
 */
@Component
class LogNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationAdapter.class);

    @Override
    public void transferCompleted(Transfer transfer) {
        Runnable send = () -> {
            try {
                log.info("Notificación: transferencia {} completada por {} {}", transfer.id(), transfer.amount(),
                        transfer.currency());
            } catch (RuntimeException e) {
                log.warn("No se pudo notificar la transferencia {}", transfer.id(), e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
