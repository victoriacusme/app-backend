package ec.nexo.accounts.infrastructure.adapter.in.web.dto;

import ec.nexo.accounts.domain.model.Account;
import ec.nexo.accounts.domain.model.AccountStatus;
import ec.nexo.accounts.domain.model.AccountType;

import java.util.UUID;

/** El número de cuenta completo nunca sale del servicio: solo su versión enmascarada. */
public record AccountResponse(UUID id, String number, AccountType type, String currency, String balance,
                              AccountStatus status, String alias, boolean isDefault) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.id(), account.maskedNumber(), account.type(), account.currency(),
                Amounts.format(account.balance()), account.status(), account.alias(), account.isDefault());
    }
}
