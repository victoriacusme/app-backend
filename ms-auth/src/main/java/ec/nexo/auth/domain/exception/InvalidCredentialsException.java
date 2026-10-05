package ec.nexo.auth.domain.exception;

public class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException() {
        super("Usuario o contraseña incorrectos");
    }
}
