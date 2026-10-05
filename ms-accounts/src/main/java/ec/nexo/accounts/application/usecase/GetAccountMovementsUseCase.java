package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.application.port.out.AccountRepositoryPort;
import ec.nexo.accounts.application.port.out.MovementRepositoryPort;
import ec.nexo.accounts.domain.exception.AccountNotFoundException;
import ec.nexo.accounts.domain.model.Movement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class GetAccountMovementsUseCase {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;

    private final AccountRepositoryPort accounts;
    private final MovementRepositoryPort movements;

    public GetAccountMovementsUseCase(AccountRepositoryPort accounts, MovementRepositoryPort movements) {
        this.accounts = accounts;
        this.movements = movements;
    }

    @Transactional(readOnly = true)
    public MovementPage execute(UUID customerId, UUID accountId, MovementCursor after, Integer requestedSize) {
        accounts.findByIdAndCustomerId(accountId, customerId).orElseThrow(AccountNotFoundException::new);

        int size = requestedSize == null ? DEFAULT_PAGE_SIZE : Math.clamp(requestedSize, 1, MAX_PAGE_SIZE);
        // Se pide uno más para saber si existe una página siguiente sin hacer un COUNT.
        List<Movement> found = movements.findPage(accountId, after, size + 1);
        if (found.size() <= size) {
            return new MovementPage(found, null);
        }
        List<Movement> page = found.subList(0, size);
        return new MovementPage(List.copyOf(page), MovementCursor.of(page.getLast()));
    }
}
