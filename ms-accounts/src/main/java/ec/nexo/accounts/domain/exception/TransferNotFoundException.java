package ec.nexo.accounts.domain.exception;

public class TransferNotFoundException extends AccountsException {

    public TransferNotFoundException() {
        super("La transferencia no existe");
    }
}
