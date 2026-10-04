package ec.nexo.auth.domain.exception;

/** Base de los errores de negocio de autenticación. */
public abstract class AuthException extends RuntimeException {

    protected AuthException(String message) {
        super(message);
    }
}
