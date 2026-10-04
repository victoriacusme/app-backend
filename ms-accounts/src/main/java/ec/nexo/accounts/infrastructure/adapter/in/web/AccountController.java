package ec.nexo.accounts.infrastructure.adapter.in.web;

import ec.nexo.accounts.application.usecase.GetAccountMovementsUseCase;
import ec.nexo.accounts.application.usecase.GetMyAccountUseCase;
import ec.nexo.accounts.application.usecase.GetMyAccountsUseCase;
import ec.nexo.accounts.application.usecase.MovementPage;
import ec.nexo.accounts.infrastructure.adapter.in.web.dto.AccountListResponse;
import ec.nexo.accounts.infrastructure.adapter.in.web.dto.AccountResponse;
import ec.nexo.accounts.infrastructure.adapter.in.web.dto.MovementPageResponse;
import ec.nexo.accounts.infrastructure.adapter.in.web.dto.MovementResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
class AccountController {

    private final GetMyAccountsUseCase getMyAccounts;
    private final GetMyAccountUseCase getMyAccount;
    private final GetAccountMovementsUseCase getAccountMovements;

    AccountController(GetMyAccountsUseCase getMyAccounts, GetMyAccountUseCase getMyAccount,
                      GetAccountMovementsUseCase getAccountMovements) {
        this.getMyAccounts = getMyAccounts;
        this.getMyAccount = getMyAccount;
        this.getAccountMovements = getAccountMovements;
    }

    @GetMapping
    AccountListResponse list(@AuthenticationPrincipal Jwt jwt) {
        return new AccountListResponse(getMyAccounts.execute(CurrentCustomer.id(jwt)).stream()
                .map(AccountResponse::from)
                .toList());
    }

    @GetMapping("/{accountId}")
    AccountResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID accountId) {
        return AccountResponse.from(getMyAccount.execute(CurrentCustomer.id(jwt), accountId));
    }

    @GetMapping("/{accountId}/movements")
    MovementPageResponse movements(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID accountId,
                                   @RequestParam(required = false) String cursor,
                                   @RequestParam(required = false) Integer size) {
        MovementPage page = getAccountMovements.execute(CurrentCustomer.id(jwt), accountId,
                MovementCursorCodec.decode(cursor), size);
        return new MovementPageResponse(page.items().stream().map(MovementResponse::from).toList(),
                MovementCursorCodec.encode(page.next()));
    }
}
