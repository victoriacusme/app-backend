package ec.nexo.accounts.domain.exception;

/** La cuenta no existe o no pertenece al cliente: ambos casos se responden igual para no revelar cuentas ajenas. */
public class AccountNotFoundException extends AccountsException {

    public AccountNotFoundException() {
        super("La cuenta no existe");
    }
}
