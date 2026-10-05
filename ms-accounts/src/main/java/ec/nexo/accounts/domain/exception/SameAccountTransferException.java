package ec.nexo.accounts.domain.exception;

public class SameAccountTransferException extends AccountsException {

    public SameAccountTransferException() {
        super("La cuenta de origen y la de destino deben ser distintas");
    }
}
