package ec.nexo.accounts.application.port.out;

/**
 * Aviso al cliente de una operación. Las implementaciones nunca deben hacer fallar la operación de negocio
 * y solo deben avisar si la transacción se confirma.
 */
public interface NotificationPort {

    void transferCompleted(TransferNotification notification);
}
