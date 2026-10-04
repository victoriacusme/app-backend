package ec.nexo.accounts.domain.exception;

public class AccountNotActiveException extends AccountsException {

    public AccountNotActiveException() {
        super("La cuenta no está activa");
    }
}
