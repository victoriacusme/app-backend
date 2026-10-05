package ec.nexo.accounts.domain.exception;

public class InsufficientFundsException extends AccountsException {

    public InsufficientFundsException() {
        super("Saldo insuficiente para realizar la operación");
    }
}
