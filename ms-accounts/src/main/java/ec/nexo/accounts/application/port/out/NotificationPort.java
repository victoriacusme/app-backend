package ec.nexo.accounts.application.port.out;

import ec.nexo.accounts.domain.model.Transfer;

/** Aviso al cliente de una operación. Las implementaciones nunca deben hacer fallar la operación de negocio. */
public interface NotificationPort {

    void transferCompleted(Transfer transfer);
}
