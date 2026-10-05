package ec.nexo.customer.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("nexo.security")
public record SecurityProperties(
        @DefaultValue("nexo-auth") String jwtIssuer,
        String jwksUri,
        @DefaultValue("2s") Duration jwksConnectTimeout,
        @DefaultValue("3s") Duration jwksReadTimeout,
        String internalApiKey) {
}
