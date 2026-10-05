package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.domain.model.Movement;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Posición en la lista de movimientos. Se pagina por (bookedAt, id) y no por número de página:
 * así un movimiento nuevo no desplaza las páginas siguientes ni duplica filas en el scroll infinito.
 */
public record MovementCursor(Instant bookedAt, UUID id) {

    public MovementCursor {
        Objects.requireNonNull(bookedAt);
        Objects.requireNonNull(id);
    }

    public static MovementCursor of(Movement movement) {
        return new MovementCursor(movement.bookedAt(), movement.id());
    }
}
