package ec.nexo.accounts.infrastructure.adapter.in.web.dto;

import ec.nexo.accounts.domain.model.Transfer;
import ec.nexo.accounts.domain.model.TransferStatus;

import java.time.Instant;
import java.util.UUID;

public record TransferResponse(UUID id, TransferStatus status, UUID sourceAccountId, UUID targetAccountId,
                               String amount, String currency, String description, Instant createdAt) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(transfer.id(), transfer.status(), transfer.sourceAccountId(),
                transfer.targetAccountId(), Amounts.format(transfer.amount()), transfer.currency(),
                transfer.description(), transfer.createdAt());
    }
}
