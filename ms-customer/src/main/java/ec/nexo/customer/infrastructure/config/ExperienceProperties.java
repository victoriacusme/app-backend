package ec.nexo.customer.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.ZoneId;

/** Zona horaria con la que se evalúan las franjas horarias y el saludo de la experiencia. */
@ConfigurationProperties("nexo.experience")
public record ExperienceProperties(@DefaultValue("America/Guayaquil") ZoneId zone) {
}
