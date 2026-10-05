package ec.nexo.accounts.application.port.out;

/** Otra petición abrió la cuenta por defecto del cliente al mismo tiempo (restricción única en BD). */
public class DuplicateDefaultAccountException extends RuntimeException {

    public DuplicateDefaultAccountException(Throwable cause) {
        super("El cliente ya tiene una cuenta por defecto", cause);
    }
}
