package ec.nexo.accounts.application.port.out;

import ec.nexo.accounts.domain.model.Transfer;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepositoryPort {

    Optional<Transfer> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    Optional<Transfer> findByIdAndCustomerId(UUID id, UUID customerId);

    /** @throws ec.nexo.accounts.domain.exception.IdempotencyKeyReusedException si la clave ya existe para el cliente */
    Transfer save(Transfer transfer);
}
