package ec.nexo.customer.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
public class SecurityConfig {

    /** {@code /internal/**}: solo servicio a servicio, con API key. El gateway bloquea estas rutas. */
    @Bean
    @Order(1)
    SecurityFilterChain internalFilterChain(HttpSecurity http, SecurityProperties properties,
                                            ObjectMapper objectMapper) throws Exception {
        return http
                .securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new InternalApiKeyFilter(properties.internalApiKey()), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("INTERNAL"))
                .exceptionHandling(e -> e.authenticationEntryPoint(new ProblemDetailAuthenticationEntryPoint(objectMapper)))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        var entryPoint = new ProblemDetailAuthenticationEntryPoint(objectMapper);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Solo el health check es público: lo usan Docker y los orquestadores, que no tienen token.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()).authenticationEntryPoint(entryPoint))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
                .build();
    }

    /**
     * Valida firma (JWKS de ms-auth), expiración y emisor. Las claves quedan en caché, así que una caída breve de
     * ms-auth no impide validar tokens; los timeouts evitan que un ms-auth lento bloquee las peticiones.
     */
    @Bean
    JwtDecoder jwtDecoder(SecurityProperties properties, RestTemplateBuilder restTemplateBuilder) {
        var restOperations = restTemplateBuilder
                .connectTimeout(properties.jwksConnectTimeout())
                .readTimeout(properties.jwksReadTimeout())
                .build();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwksUri())
                .restOperations(restOperations)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.jwtIssuer()));
        return decoder;
    }
}
