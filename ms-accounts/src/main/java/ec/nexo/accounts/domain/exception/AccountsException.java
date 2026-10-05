package ec.nexo.accounts.domain.exception;

/** Base de los errores de negocio de cuentas. */
public abstract class AccountsException extends RuntimeException {

    protected AccountsException(String message) {
        super(message);
    }
}
