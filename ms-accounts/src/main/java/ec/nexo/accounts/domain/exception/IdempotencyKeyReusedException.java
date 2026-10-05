package ec.nexo.accounts.domain.exception;

/** La misma clave con un contenido distinto: casi siempre es un error del cliente al generar las claves. */
public class IdempotencyKeyReusedException extends AccountsException {

    public IdempotencyKeyReusedException() {
        super("La Idempotency-Key ya se usó con otros datos");
    }
}
