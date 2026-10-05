package ec.nexo.accounts.infrastructure.config;

import ec.nexo.accounts.application.port.out.AccountRepositoryPort;
import ec.nexo.accounts.application.port.out.MovementRepositoryPort;
import ec.nexo.accounts.application.port.out.NotificationPort;
import ec.nexo.accounts.application.port.out.TransferRepositoryPort;
import ec.nexo.accounts.application.usecase.GetAccountMovementsUseCase;
import ec.nexo.accounts.application.usecase.GetMyAccountUseCase;
import ec.nexo.accounts.application.usecase.GetMyAccountsUseCase;
import ec.nexo.accounts.application.usecase.GetTransferUseCase;
import ec.nexo.accounts.application.usecase.OpenDefaultAccountUseCase;
import ec.nexo.accounts.application.usecase.TransferBetweenOwnAccountsUseCase;
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
    GetMyAccountsUseCase getMyAccountsUseCase(AccountRepositoryPort accounts) {
        return new GetMyAccountsUseCase(accounts);
    }

    @Bean
    GetMyAccountUseCase getMyAccountUseCase(AccountRepositoryPort accounts) {
        return new GetMyAccountUseCase(accounts);
    }

    @Bean
    GetAccountMovementsUseCase getAccountMovementsUseCase(AccountRepositoryPort accounts,
                                                          MovementRepositoryPort movements) {
        return new GetAccountMovementsUseCase(accounts, movements);
    }

    @Bean
    OpenDefaultAccountUseCase openDefaultAccountUseCase(AccountRepositoryPort accounts, Clock clock) {
        return new OpenDefaultAccountUseCase(accounts, clock);
    }

    @Bean
    TransferBetweenOwnAccountsUseCase transferBetweenOwnAccountsUseCase(AccountRepositoryPort accounts,
                                                                        MovementRepositoryPort movements,
                                                                        TransferRepositoryPort transfers,
                                                                        NotificationPort notifications,
                                                                        Clock clock) {
        return new TransferBetweenOwnAccountsUseCase(accounts, movements, transfers, notifications, clock);
    }

    @Bean
    GetTransferUseCase getTransferUseCase(TransferRepositoryPort transfers) {
        return new GetTransferUseCase(transfers);
    }
}
