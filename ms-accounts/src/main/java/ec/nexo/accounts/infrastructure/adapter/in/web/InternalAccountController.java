package ec.nexo.accounts.infrastructure.adapter.in.web;

import ec.nexo.accounts.application.usecase.OpenDefaultAccountUseCase;
import ec.nexo.accounts.infrastructure.adapter.in.web.dto.AccountResponse;
import ec.nexo.accounts.infrastructure.adapter.in.web.dto.OpenAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints servicio a servicio (API key). El gateway no los expone. */
@RestController
@RequestMapping("/internal/accounts")
class InternalAccountController {

    private final OpenDefaultAccountUseCase openDefaultAccount;

    InternalAccountController(OpenDefaultAccountUseCase openDefaultAccount) {
        this.openDefaultAccount = openDefaultAccount;
    }

    /** 201 si se abrió la cuenta, 200 si ya existía (reintento idempotente de ms-auth). */
    @PostMapping
    ResponseEntity<AccountResponse> openDefault(@Valid @RequestBody OpenAccountRequest request) {
        var result = openDefaultAccount.execute(request.customerId());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(AccountResponse.from(result.account()));
    }
}
