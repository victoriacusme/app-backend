package ec.nexo.customer.infrastructure.config;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;
import ec.nexo.customer.application.port.out.DeviceTokenRepositoryPort;
import ec.nexo.customer.application.port.out.ExperienceRepositoryPort;
import ec.nexo.customer.application.port.out.PreferencesRepositoryPort;
import ec.nexo.customer.application.port.out.PushSenderPort;
import ec.nexo.customer.application.usecase.DiscardCustomerUseCase;
import ec.nexo.customer.application.usecase.GetHomeExperienceUseCase;
import ec.nexo.customer.application.usecase.GetMyProfileUseCase;
import ec.nexo.customer.application.usecase.NotifyCustomerUseCase;
import ec.nexo.customer.application.usecase.ProvisionCustomerUseCase;
import ec.nexo.customer.application.usecase.RegisterDeviceUseCase;
import ec.nexo.customer.application.usecase.UnregisterDeviceUseCase;
import ec.nexo.customer.application.usecase.UpdatePreferencesUseCase;
import ec.nexo.customer.domain.service.ExperienceComposer;
import ec.nexo.customer.domain.service.NotificationTemplates;
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
    GetMyProfileUseCase getMyProfileUseCase(CustomerRepositoryPort customers, PreferencesRepositoryPort preferences) {
        return new GetMyProfileUseCase(customers, preferences);
    }

    @Bean
    UpdatePreferencesUseCase updatePreferencesUseCase(CustomerRepositoryPort customers,
                                                      PreferencesRepositoryPort preferences) {
        return new UpdatePreferencesUseCase(customers, preferences);
    }

    @Bean
    GetHomeExperienceUseCase getHomeExperienceUseCase(CustomerRepositoryPort customers,
                                                      PreferencesRepositoryPort preferences,
                                                      ExperienceRepositoryPort experiences, Clock clock,
                                                      ExperienceProperties properties) {
        return new GetHomeExperienceUseCase(customers, preferences, experiences, new ExperienceComposer(), clock,
                properties.zone());
    }

    @Bean
    ProvisionCustomerUseCase provisionCustomerUseCase(CustomerRepositoryPort customers, Clock clock,
                                                      ExperienceProperties properties) {
        return new ProvisionCustomerUseCase(customers, clock, properties.zone());
    }

    @Bean
    DiscardCustomerUseCase discardCustomerUseCase(CustomerRepositoryPort customers) {
        return new DiscardCustomerUseCase(customers);
    }

    @Bean
    RegisterDeviceUseCase registerDeviceUseCase(CustomerRepositoryPort customers, DeviceTokenRepositoryPort devices) {
        return new RegisterDeviceUseCase(customers, devices);
    }

    @Bean
    UnregisterDeviceUseCase unregisterDeviceUseCase(DeviceTokenRepositoryPort devices) {
        return new UnregisterDeviceUseCase(devices);
    }

    @Bean
    NotifyCustomerUseCase notifyCustomerUseCase(CustomerRepositoryPort customers,
                                                PreferencesRepositoryPort preferences,
                                                DeviceTokenRepositoryPort devices, PushSenderPort pushSender) {
        return new NotifyCustomerUseCase(customers, preferences, devices, pushSender, new NotificationTemplates());
    }
}
