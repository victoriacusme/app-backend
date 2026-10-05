package ec.nexo.accounts.infrastructure.adapter.in.web.dto;

import ec.nexo.accounts.domain.model.Movement;
import ec.nexo.accounts.domain.model.MovementType;

import java.time.Instant;
import java.util.UUID;

public record MovementResponse(UUID id, MovementType type, String amount, String balanceAfter, String description,
                               Instant bookedAt) {

    public static MovementResponse from(Movement movement) {
        return new MovementResponse(movement.id(), movement.type(), Amounts.format(movement.amount()),
                Amounts.format(movement.balanceAfter()), movement.description(), movement.bookedAt());
    }
}
