package ec.nexo.accounts.infrastructure.adapter.in.web;

import ec.nexo.accounts.application.usecase.GetTransferUseCase;
import ec.nexo.accounts.application.usecase.TransferBetweenOwnAccountsUseCase;
import ec.nexo.accounts.application.usecase.TransferBetweenOwnAccountsUseCase.TransferCommand;
import ec.nexo.accounts.infrastructure.adapter.in.web.dto.TransferRequest;
import ec.nexo.accounts.infrastructure.adapter.in.web.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Transferencias entre cuentas propias. Si la app no recibe respuesta (timeout), debe reenviar la misma solicitud
 * con la misma {@code Idempotency-Key}: nunca se ejecuta dos veces y devuelve el resultado original.
 */
@RestController
@RequestMapping("/transfers")
class TransferController {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    static final String REPLAYED = "Idempotent-Replayed";
    private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final TransferBetweenOwnAccountsUseCase transferBetweenOwnAccounts;
    private final GetTransferUseCase getTransfer;

    TransferController(TransferBetweenOwnAccountsUseCase transferBetweenOwnAccounts, GetTransferUseCase getTransfer) {
        this.transferBetweenOwnAccounts = transferBetweenOwnAccounts;
        this.getTransfer = getTransfer;
    }

    /** 201 la primera vez; 200 con {@code Idempotent-Replayed: true} si la clave ya se había procesado. */
    @PostMapping("/own")
    ResponseEntity<TransferResponse> transferBetweenOwnAccounts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        if (idempotencyKey == null || !VALID_KEY.matcher(idempotencyKey).matches()) {
            throw new InvalidIdempotencyKeyException();
        }
        var command = new TransferCommand(CurrentCustomer.id(jwt), idempotencyKey, request.sourceAccountId(),
                request.targetAccountId(), new BigDecimal(request.amount()), blankToNull(request.description()));
        var result = transferBetweenOwnAccounts.execute(command);
        var body = TransferResponse.from(result.transfer());
        if (result.replayed()) {
            return ResponseEntity.ok().header(REPLAYED, "true").body(body);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/transfers/" + body.id()))
                .body(body);
    }

    @GetMapping("/{transferId}")
    TransferResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID transferId) {
        return TransferResponse.from(getTransfer.execute(CurrentCustomer.id(jwt), transferId));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
