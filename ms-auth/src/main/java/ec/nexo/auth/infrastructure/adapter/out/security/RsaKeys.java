package ec.nexo.auth.infrastructure.adapter.out.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import ec.nexo.auth.infrastructure.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * Par de claves RSA del servicio: una para firmar JWT ({@code sig}) y otra para que la app cifre
 * el login ({@code enc}). Las claves públicas se publican en el JWKS.
 */
@Component
public class RsaKeys {

    private static final Logger log = LoggerFactory.getLogger(RsaKeys.class);

    private final RSAKey signingKey;
    private final RSAKey encryptionKey;

    public RsaKeys(SecurityProperties properties) {
        this.signingKey = load(properties.signingKey(), KeyUse.SIGNATURE, JWSAlgorithm.RS256, "JWT_SIGNING_KEY");
        this.encryptionKey = load(properties.encryptionKey(), KeyUse.ENCRYPTION, JWEAlgorithm.RSA_OAEP_256,
                "JWT_ENCRYPTION_KEY");
    }

    public RSAKey signingKey() {
        return signingKey;
    }

    public RSAKey encryptionKey() {
        return encryptionKey;
    }

    public JWKSet publicJwkSet() {
        return new JWKSet(List.of(signingKey.toPublicJWK(), encryptionKey.toPublicJWK()));
    }

    private static RSAKey load(String pem, KeyUse use, com.nimbusds.jose.Algorithm alg, String envName) {
        try {
            RSAKey.Builder builder;
            if (pem == null || pem.isBlank()) {
                log.warn("{} no está configurada: se genera una clave efímera ({}). "
                        + "Los tokens emitidos dejan de ser válidos al reiniciar.", envName, use.identifier());
                builder = new RSAKey.Builder(new RSAKeyGenerator(2048).generate());
            } else {
                RSAPrivateCrtKey privateKey = parsePrivateKey(pem);
                RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                        .generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
                builder = new RSAKey.Builder(publicKey).privateKey(privateKey);
            }
            RSAKey key = builder.keyUse(use).algorithm(alg).build();
            return new RSAKey.Builder(key).keyID(use.identifier() + "-" + key.computeThumbprint()).build();
        } catch (GeneralSecurityException | JOSEException | IllegalArgumentException | ClassCastException e) {
            throw new IllegalStateException("No se pudo cargar la clave " + envName
                    + " (se espera una clave privada RSA en PEM PKCS#8)", e);
        }
    }

    private static RSAPrivateCrtKey parsePrivateKey(String pem) throws GeneralSecurityException {
        String base64 = pem.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        return (RSAPrivateCrtKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }
}
