package ec.nexo.customer.domain.exception;

public class CustomerNotFoundException extends CustomerException {

    public CustomerNotFoundException() {
        super("El cliente no existe");
    }
}
