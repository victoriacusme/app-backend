package ec.nexo.auth.application.usecase;

import ec.nexo.auth.application.port.out.PasswordHasherPort;
import ec.nexo.auth.application.port.out.UserRepositoryPort;
import ec.nexo.auth.domain.exception.AuthException;
import ec.nexo.auth.domain.exception.InvalidCredentialsException;
import ec.nexo.auth.domain.exception.UserLockedException;
import ec.nexo.auth.domain.model.AuthTokens;
import ec.nexo.auth.domain.model.LockoutPolicy;
import ec.nexo.auth.domain.model.User;
import org.springframework.transaction.annotation.Transactional;

public class LoginUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final SessionIssuer sessionIssuer;
    private final LockoutPolicy lockoutPolicy;
    private volatile String dummyHash;

    public LoginUseCase(UserRepositoryPort users, PasswordHasherPort passwordHasher,
                        SessionIssuer sessionIssuer, LockoutPolicy lockoutPolicy) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.sessionIssuer = sessionIssuer;
        this.lockoutPolicy = lockoutPolicy;
    }

    // Los intentos fallidos se guardan aunque el login termine en excepción, por eso no se hace rollback.
    @Transactional(noRollbackFor = AuthException.class)
    public AuthTokens execute(LoginCommand command) {
        String username = User.normalizeUsername(command.username());
        User user = users.findByUsernameForUpdate(username).orElse(null);
        if (user == null) {
            // Se verifica igual contra un hash ficticio para no revelar por tiempo de respuesta si el usuario existe.
            passwordHasher.matches(command.password(), dummyHash());
            throw new InvalidCredentialsException();
        }
        if (user.isLocked()) {
            throw new UserLockedException();
        }
        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            user.registerFailedAttempt(lockoutPolicy);
            users.save(user);
            throw user.isLocked() ? new UserLockedException() : new InvalidCredentialsException();
        }
        if (user.failedAttempts() > 0) {
            user.registerSuccessfulLogin();
            users.save(user);
        }
        return sessionIssuer.open(user, command.deviceId());
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordHasher.hash("nexo-dummy-password");
        }
        return dummyHash;
    }

    public record LoginCommand(String username, String password, String deviceId) {

        /** Nunca imprime la contraseña. */
        @Override
        public String toString() {
            return "LoginCommand[username=" + username + ", deviceId=" + deviceId + "]";
        }
    }
}
