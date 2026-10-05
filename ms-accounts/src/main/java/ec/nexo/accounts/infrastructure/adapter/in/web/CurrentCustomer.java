package ec.nexo.accounts.infrastructure.adapter.in.web;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.UUID;

/** El cliente siempre se toma del {@code sub} del JWT, nunca de un parámetro de la petición. */
final class CurrentCustomer {

    private CurrentCustomer() {
    }

    static UUID id(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidBearerTokenException("El token no identifica a un cliente");
        }
    }
}
