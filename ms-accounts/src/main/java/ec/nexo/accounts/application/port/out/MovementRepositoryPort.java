package ec.nexo.accounts.application.port.out;

import ec.nexo.accounts.application.usecase.MovementCursor;
import ec.nexo.accounts.domain.model.Movement;

import java.util.List;
import java.util.UUID;

public interface MovementRepositoryPort {

    /** Movimientos del más reciente al más antiguo, a partir del cursor (excluido) o desde el inicio si es nulo. */
    List<Movement> findPage(UUID accountId, MovementCursor after, int limit);
}
