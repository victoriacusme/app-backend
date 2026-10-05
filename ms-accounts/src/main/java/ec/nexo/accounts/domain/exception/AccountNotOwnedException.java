package ec.nexo.accounts.domain.exception;

public class AccountNotOwnedException extends AccountsException {

    public AccountNotOwnedException() {
        super("La cuenta no pertenece al cliente");
    }
}
