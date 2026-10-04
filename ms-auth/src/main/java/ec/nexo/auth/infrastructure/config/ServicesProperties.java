package ec.nexo.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Servicios internos a los que llama el onboarding. */
@ConfigurationProperties("nexo.services")
public record ServicesProperties(
        String customerUrl,
        String accountsUrl,
        String internalApiKey,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout) {
}
