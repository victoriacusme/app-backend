package ec.nexo.accounts.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Servicios a los que llama ms-accounts (hoy, ms-customer para las notificaciones). */
@ConfigurationProperties("nexo.services")
public record ServicesProperties(
        String customerUrl,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout) {
}
