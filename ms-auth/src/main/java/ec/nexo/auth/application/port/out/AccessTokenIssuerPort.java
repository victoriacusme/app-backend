package ec.nexo.auth.application.port.out;

import ec.nexo.auth.domain.model.User;

import java.time.Duration;

public interface AccessTokenIssuerPort {

    IssuedAccessToken issue(User user);

    record IssuedAccessToken(String value, Duration ttl) {
    }
}
