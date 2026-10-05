package ec.nexo.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * @param signingKey    clave privada RSA (PEM PKCS#8) para firmar los JWT; si está vacía se genera una efímera
 * @param encryptionKey clave privada RSA (PEM PKCS#8) para descifrar el login JWE; si está vacía se genera una efímera
 */
@ConfigurationProperties("nexo.security")
public record SecurityProperties(
        @DefaultValue("nexo-auth") String issuer,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("7d") Duration refreshTokenTtl,
        @DefaultValue("5") int maxFailedAttempts,
        String signingKey,
        String encryptionKey) {
}
