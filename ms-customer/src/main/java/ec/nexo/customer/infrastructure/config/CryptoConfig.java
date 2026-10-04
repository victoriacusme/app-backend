package ec.nexo.customer.infrastructure.config;

import ec.nexo.customer.infrastructure.adapter.out.persistence.crypto.FieldEncryptor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CryptoConfig {

    /** Sin una clave válida el servicio no arranca: es preferible a guardar datos personales en claro. */
    @Bean
    FieldEncryptor fieldEncryptor(CryptoProperties properties) {
        return new FieldEncryptor(properties.dataKey());
    }

    @ConfigurationProperties("nexo.crypto")
    record CryptoProperties(String dataKey) {
    }
}
