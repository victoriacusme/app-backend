package ec.nexo.auth.infrastructure.config;

import ec.nexo.auth.application.port.out.AccessTokenIssuerPort;
import ec.nexo.auth.application.port.out.AccountProvisioningPort;
import ec.nexo.auth.application.port.out.CustomerProvisioningPort;
import ec.nexo.auth.application.port.out.PasswordHasherPort;
import ec.nexo.auth.application.port.out.RefreshTokenRepositoryPort;
import ec.nexo.auth.application.port.out.UserRepositoryPort;
import ec.nexo.auth.application.usecase.LoginUseCase;
import ec.nexo.auth.application.usecase.LogoutUseCase;
import ec.nexo.auth.application.usecase.RefreshTokenUseCase;
import ec.nexo.auth.application.usecase.RegisterUseCase;
import ec.nexo.auth.application.usecase.SessionIssuer;
import ec.nexo.auth.domain.model.LockoutPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Ensambla los casos de uso: la capa de aplicación no depende de anotaciones de componentes de Spring. */
@Configuration
class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SessionIssuer sessionIssuer(AccessTokenIssuerPort accessTokenIssuer, RefreshTokenRepositoryPort refreshTokens,
                                SecurityProperties properties, Clock clock) {
        return new SessionIssuer(accessTokenIssuer, refreshTokens, properties.refreshTokenTtl(), clock);
    }

    @Bean
    LoginUseCase loginUseCase(UserRepositoryPort users, PasswordHasherPort passwordHasher,
                              SessionIssuer sessionIssuer, SecurityProperties properties) {
        return new LoginUseCase(users, passwordHasher, sessionIssuer,
                new LockoutPolicy(properties.maxFailedAttempts()));
    }

    @Bean
    RegisterUseCase registerUseCase(UserRepositoryPort users, PasswordHasherPort passwordHasher,
                                    CustomerProvisioningPort customerProvisioning,
                                    AccountProvisioningPort accountProvisioning,
                                    SessionIssuer sessionIssuer, Clock clock) {
        return new RegisterUseCase(users, passwordHasher, customerProvisioning, accountProvisioning,
                sessionIssuer, clock);
    }

    @Bean
    RefreshTokenUseCase refreshTokenUseCase(RefreshTokenRepositoryPort refreshTokens, UserRepositoryPort users,
                                            SessionIssuer sessionIssuer, Clock clock) {
        return new RefreshTokenUseCase(refreshTokens, users, sessionIssuer, clock);
    }

    @Bean
    LogoutUseCase logoutUseCase(RefreshTokenRepositoryPort refreshTokens) {
        return new LogoutUseCase(refreshTokens);
    }
}
