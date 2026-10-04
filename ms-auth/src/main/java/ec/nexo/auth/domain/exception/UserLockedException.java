package ec.nexo.auth.domain.exception;

public class UserLockedException extends AuthException {

    public UserLockedException() {
        super("El usuario está bloqueado por exceso de intentos fallidos");
    }
}
