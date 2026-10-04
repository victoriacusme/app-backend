package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.application.port.out.AccountRepositoryPort;
import ec.nexo.accounts.application.port.out.MovementRepositoryPort;
import ec.nexo.accounts.application.port.out.NotificationPort;
import ec.nexo.accounts.application.port.out.TransferNotification;
import ec.nexo.accounts.application.port.out.TransferRepositoryPort;
import ec.nexo.accounts.domain.exception.AccountNotFoundException;
import ec.nexo.accounts.domain.exception.AccountNotOwnedException;
import ec.nexo.accounts.domain.exception.CurrencyMismatchException;
import ec.nexo.accounts.domain.exception.IdempotencyKeyReusedException;
import ec.nexo.accounts.domain.exception.InvalidAmountException;
import ec.nexo.accounts.domain.exception.SameAccountTransferException;
import ec.nexo.accounts.domain.model.Account;
import ec.nexo.accounts.domain.model.Movement;
import ec.nexo.accounts.domain.model.Transfer;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transferencia entre cuentas del mismo cliente, liquidada en una sola transacción:
 * bloqueo de ambas cuentas → validaciones → débito y crédito → transferencia y dos movimientos.
 * Es idempotente por {@code Idempotency-Key}: repetir la solicitud devuelve la transferencia original.
 */
public class TransferBetweenOwnAccountsUseCase {

    private static final int MAX_SCALE = 2;

    private final AccountRepositoryPort accounts;
    private final MovementRepositoryPort movements;
    private final TransferRepositoryPort transfers;
    private final NotificationPort notifications;
    private final Clock clock;

    public TransferBetweenOwnAccountsUseCase(AccountRepositoryPort accounts, MovementRepositoryPort movements,
                                             TransferRepositoryPort transfers, NotificationPort notifications,
                                             Clock clock) {
        this.accounts = accounts;
        this.movements = movements;
        this.transfers = transfers;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public Result execute(TransferCommand command) {
        validate(command);
        String requestHash = Transfer.fingerprint(command.sourceAccountId(), command.targetAccountId(),
                command.amount(), command.description());

        // Camino rápido: un reintento de una transferencia ya completada no necesita bloquear nada.
        Optional<Result> replay = findReplay(command, requestHash);
        if (replay.isPresent()) {
            return replay.get();
        }

        Map<UUID, Account> locked = accounts
                .findAllByIdForUpdate(Set.of(command.sourceAccountId(), command.targetAccountId())).stream()
                .collect(Collectors.toMap(Account::id, Function.identity()));
        Account source = owned(locked.get(command.sourceAccountId()), command.customerId());
        Account target = owned(locked.get(command.targetAccountId()), command.customerId());

        // Con las cuentas bloqueadas se vuelve a mirar: una solicitud simultánea con la misma clave
        // pudo completarse mientras esperábamos el bloqueo.
        replay = findReplay(command, requestHash);
        if (replay.isPresent()) {
            return replay.get();
        }

        if (!source.currency().equals(target.currency())) {
            throw new CurrencyMismatchException();
        }
        source.debit(command.amount());
        target.credit(command.amount());

        Transfer transfer = transfers.save(Transfer.completed(command.customerId(), command.idempotencyKey(),
                requestHash, source, target, command.amount(), command.description(), clock.instant()));
        accounts.updateBalance(source);
        accounts.updateBalance(target);
        movements.saveAll(List.of(Movement.debitOf(transfer, source, target),
                Movement.creditOf(transfer, source, target)));
        notifications.transferCompleted(new TransferNotification(command.customerId(), transfer.id(),
                transfer.amount(), transfer.currency(), target.maskedNumber()));
        return new Result(transfer, false);
    }

    private static void validate(TransferCommand command) {
        BigDecimal amount = command.amount();
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > MAX_SCALE) {
            throw new InvalidAmountException();
        }
        if (command.sourceAccountId().equals(command.targetAccountId())) {
            throw new SameAccountTransferException();
        }
    }

    private Optional<Result> findReplay(TransferCommand command, String requestHash) {
        return transfers.findByCustomerIdAndIdempotencyKey(command.customerId(), command.idempotencyKey())
                .map(existing -> {
                    if (!existing.requestHash().equals(requestHash)) {
                        throw new IdempotencyKeyReusedException();
                    }
                    return new Result(existing, true);
                });
    }

    private static Account owned(Account account, UUID customerId) {
        if (account == null) {
            throw new AccountNotFoundException();
        }
        if (!account.isOwnedBy(customerId)) {
            throw new AccountNotOwnedException();
        }
        return account;
    }

    public record TransferCommand(UUID customerId, String idempotencyKey, UUID sourceAccountId,
                                  UUID targetAccountId, BigDecimal amount, String description) {
    }

    /** {@code replayed} indica que la transferencia ya existía y se devuelve sin volver a ejecutarla. */
    public record Result(Transfer transfer, boolean replayed) {
    }
}
