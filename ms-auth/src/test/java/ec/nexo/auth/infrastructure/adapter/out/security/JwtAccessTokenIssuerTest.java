package ec.nexo.auth.infrastructure.adapter.out.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import ec.nexo.auth.domain.model.User;
import ec.nexo.auth.infrastructure.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAccessTokenIssuerTest {

    private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

    @Mock
    private RsaKeys keys;
    @Mock
    private Clock clock;

    @Test
    void emiteUnJwtRs256FirmadoConLaClaveDeFirma() throws Exception {
        RSAKey signingKey = new RSAKeyGenerator(2048).keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.RS256)
                .keyID("sig-test").generate();
        when(keys.signingKey()).thenReturn(signingKey);
        when(clock.instant()).thenReturn(NOW);
        var properties = new SecurityProperties("nexo-auth", Duration.ofMinutes(15), Duration.ofDays(7), 5, null, null);
        var issuer = new JwtAccessTokenIssuer(keys, properties, clock);
        UUID customerId = UUID.randomUUID();
        User user = User.register("carlos", customerId, "hash", NOW);

        var token = issuer.issue(user);
        SignedJWT jwt = SignedJWT.parse(token.value());

        assertThat(jwt.verify(new RSASSAVerifier(signingKey.toPublicJWK()))).isTrue();
        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(jwt.getHeader().getKeyID()).isEqualTo("sig-test");
        var claims = jwt.getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("nexo-auth");
        assertThat(claims.getSubject()).isEqualTo(customerId.toString());
        assertThat(claims.getStringClaim("username")).isEqualTo("carlos");
        assertThat(claims.getStringClaim("scope")).contains("transfers:write");
        assertThat(claims.getIssueTime()).isEqualTo(Date.from(NOW));
        assertThat(claims.getExpirationTime()).isEqualTo(Date.from(NOW.plus(Duration.ofMinutes(15))));
        assertThat(token.ttl()).isEqualTo(Duration.ofMinutes(15));
    }
}
