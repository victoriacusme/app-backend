package ec.nexo.accounts.domain.exception;

public class CurrencyMismatchException extends AccountsException {

    public CurrencyMismatchException() {
        super("Las cuentas deben tener la misma moneda");
    }
}
