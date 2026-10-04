package ec.nexo.accounts.domain.exception;

public class InvalidAmountException extends AccountsException {

    public InvalidAmountException() {
        super("El monto debe ser mayor a cero y tener como máximo dos decimales");
    }
}
