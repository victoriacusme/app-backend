package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.domain.model.Movement;

import java.util.List;

/** Una página de movimientos; {@code next} es nulo cuando no hay más. */
public record MovementPage(List<Movement> items, MovementCursor next) {
}
