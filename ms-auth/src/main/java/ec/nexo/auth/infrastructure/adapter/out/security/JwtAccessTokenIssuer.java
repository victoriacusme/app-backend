package ec.nexo.auth.infrastructure.adapter.out.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import ec.nexo.auth.application.port.out.AccessTokenIssuerPort;
import ec.nexo.auth.domain.model.User;
import ec.nexo.auth.infrastructure.config.SecurityProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** Access token JWT firmado con RS256. {@code sub} es el customerId que usan ms-customer y ms-accounts. */
@Component
class JwtAccessTokenIssuer implements AccessTokenIssuerPort {

    static final String SCOPE = "customer:read customer:write accounts:read transfers:write";

    private final RsaKeys keys;
    private final RSASSASigner signer;
    private final String issuer;
    private final Duration ttl;
    private final Clock clock;

    JwtAccessTokenIssuer(RsaKeys keys, SecurityProperties properties, Clock clock) throws JOSEException {
        this.keys = keys;
        this.signer = new RSASSASigner(keys.signingKey());
        this.issuer = properties.issuer();
        this.ttl = properties.accessTokenTtl();
        this.clock = clock;
    }

    @Override
    public IssuedAccessToken issue(User user) {
        Instant now = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(user.customerId().toString())
                .claim("username", user.username())
                .claim("scope", SCOPE)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ttl)))
                .jwtID(UUID.randomUUID().toString())
                .build();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .keyID(keys.signingKey().getKeyID())
                .build();
        try {
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            return new IssuedAccessToken(jwt.serialize(), ttl);
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el access token", e);
        }
    }
}
