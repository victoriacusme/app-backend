package ec.nexo.auth.domain.exception;

public class UsernameTakenException extends AuthException {

    public UsernameTakenException(String username) {
        super("El usuario '" + username + "' ya existe");
    }
}
