package ec.nexo.customer.domain.exception;

/** Base de los errores de negocio de clientes. */
public abstract class CustomerException extends RuntimeException {

    protected CustomerException(String message) {
        super(message);
    }
}
