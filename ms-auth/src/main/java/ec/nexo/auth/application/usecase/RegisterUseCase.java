package ec.nexo.auth.application.usecase;

import ec.nexo.auth.application.port.out.AccountProvisioningPort;
import ec.nexo.auth.application.port.out.CustomerProvisioningPort;
import ec.nexo.auth.application.port.out.CustomerProvisioningPort.NewCustomer;
import ec.nexo.auth.application.port.out.PasswordHasherPort;
import ec.nexo.auth.application.port.out.ProvisioningFailedException;
import ec.nexo.auth.application.port.out.UserRepositoryPort;
import ec.nexo.auth.domain.exception.UsernameTakenException;
import ec.nexo.auth.domain.model.AuthTokens;
import ec.nexo.auth.domain.model.User;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Onboarding síncrono con compensación: provisiona el cliente y su cuenta por defecto y, solo si ambos
 * responden bien, crea las credenciales. No se abre una transacción de BD mientras se espera a los otros servicios.
 * Si la cuenta no se puede abrir, se descarta el cliente recién creado (mejor esfuerzo). Un fallo nunca deja
 * credenciales sin cliente: como mucho, si también falla la compensación, queda un cliente sin usuario,
 * que no puede autenticarse. Cada intento usa un {@code customerId} nuevo, así que reintentar es seguro.
 */
public class RegisterUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final CustomerProvisioningPort customerProvisioning;
    private final AccountProvisioningPort accountProvisioning;
    private final SessionIssuer sessionIssuer;
    private final Clock clock;

    public RegisterUseCase(UserRepositoryPort users, PasswordHasherPort passwordHasher,
                           CustomerProvisioningPort customerProvisioning,
                           AccountProvisioningPort accountProvisioning,
                           SessionIssuer sessionIssuer, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.customerProvisioning = customerProvisioning;
        this.accountProvisioning = accountProvisioning;
        this.sessionIssuer = sessionIssuer;
        this.clock = clock;
    }

    public AuthTokens execute(RegisterCommand command) {
        String username = User.normalizeUsername(command.username());
        if (users.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }

        UUID customerId = UUID.randomUUID();
        customerProvisioning.provision(new NewCustomer(customerId, command.fullName(), command.idNumber(),
                command.email(), command.phone(), command.birthDate()));
        try {
            accountProvisioning.openDefaultAccount(customerId);
        } catch (ProvisioningFailedException e) {
            discardCustomer(customerId, e);
            throw e;
        }

        User user = users.save(User.register(username, customerId, passwordHasher.hash(command.password()),
                clock.instant()));
        return sessionIssuer.open(user, command.deviceId());
    }

    private void discardCustomer(UUID customerId, ProvisioningFailedException cause) {
        try {
            customerProvisioning.discard(customerId);
        } catch (RuntimeException compensationError) {
            // No oculta el error original: queda adjunto para el log.
            cause.addSuppressed(compensationError);
        }
    }

    public record RegisterCommand(String username, String password, String fullName, String idNumber,
                                  String email, String phone, LocalDate birthDate, String deviceId) {

        /** Nunca imprime la contraseña ni datos personales. */
        @Override
        public String toString() {
            return "RegisterCommand[username=" + username + ", deviceId=" + deviceId + "]";
        }
    }
}
