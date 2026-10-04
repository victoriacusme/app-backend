package ec.nexo.auth.domain.exception;

public class InvalidRefreshTokenException extends AuthException {

    public InvalidRefreshTokenException() {
        super("El refresh token no es válido");
    }
}
