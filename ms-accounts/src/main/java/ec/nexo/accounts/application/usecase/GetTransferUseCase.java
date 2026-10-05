package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.application.port.out.TransferRepositoryPort;
import ec.nexo.accounts.domain.exception.TransferNotFoundException;
import ec.nexo.accounts.domain.model.Transfer;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Estado de una transferencia del cliente: lo usa la app cuando no supo si su solicitud llegó (timeout). */
public class GetTransferUseCase {

    private final TransferRepositoryPort transfers;

    public GetTransferUseCase(TransferRepositoryPort transfers) {
        this.transfers = transfers;
    }

    @Transactional(readOnly = true)
    public Transfer execute(UUID customerId, UUID transferId) {
        return transfers.findByIdAndCustomerId(transferId, customerId).orElseThrow(TransferNotFoundException::new);
    }
}
