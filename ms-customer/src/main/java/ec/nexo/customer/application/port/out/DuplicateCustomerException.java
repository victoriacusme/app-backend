package ec.nexo.customer.application.port.out;

/** Otra petición dio de alta al mismo cliente al mismo tiempo (clave primaria). */
public class DuplicateCustomerException extends RuntimeException {

    public DuplicateCustomerException(Throwable cause) {
        super("El cliente ya existe", cause);
    }
}
