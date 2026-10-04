package ec.nexo.auth.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import ec.nexo.auth.infrastructure.adapter.in.web.dto.LoginRequest;
import ec.nexo.auth.infrastructure.adapter.out.security.RsaKeys;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JweLoginDecrypterTest {

    private static RSAKey encryptionKey;

    @Mock
    private RsaKeys keys;
    @Mock
    private Validator validator;
    @Mock
    private ConstraintViolation<LoginRequest> violation;

    private JweLoginDecrypter decrypter;

    @BeforeAll
    static void generateKey() throws Exception {
        encryptionKey = new RSAKeyGenerator(2048).keyUse(KeyUse.ENCRYPTION).algorithm(JWEAlgorithm.RSA_OAEP_256)
                .keyID("enc-test").generate();
    }

    @BeforeEach
    void setUp() throws Exception {
        when(keys.encryptionKey()).thenReturn(encryptionKey);
        decrypter = new JweLoginDecrypter(keys, new ObjectMapper(), validator);
    }

    @Test
    void descifraUnLoginCifradoConLaClavePublica() throws Exception {
        String jwe = encrypt(encryptionKey.toPublicJWK(), JWEAlgorithm.RSA_OAEP_256,
                "{\"username\":\"ana\",\"password\":\"Nexo2026*\",\"deviceId\":\"pixel-8\"}");

        LoginRequest request = decrypter.decrypt(jwe);

        assertThat(request).isEqualTo(new LoginRequest("ana", "Nexo2026*", "pixel-8"));
        verify(validator).validate(request);
    }

    @Test
    void rechazaAlgoritmosDistintosDeRsaOaep256() throws Exception {
        String jwe = encrypt(encryptionKey.toPublicJWK(), JWEAlgorithm.RSA_OAEP_512,
                "{\"username\":\"ana\",\"password\":\"x\"}");

        assertThatThrownBy(() -> decrypter.decrypt(jwe)).isInstanceOf(InvalidEncryptedPayloadException.class);
        verifyNoInteractions(validator);
    }

    @Test
    void rechazaUnaClaveDeCifradoQueNoEsLaVigente() throws Exception {
        RSAKey otherKey = new RSAKeyGenerator(2048).keyID("enc-antigua").generate();
        String jwe = encrypt(otherKey.toPublicJWK(), JWEAlgorithm.RSA_OAEP_256,
                "{\"username\":\"ana\",\"password\":\"x\"}");

        assertThatThrownBy(() -> decrypter.decrypt(jwe)).isInstanceOf(InvalidEncryptedPayloadException.class);
        verifyNoInteractions(validator);
    }

    @Test
    void rechazaContenidoQueNoEsJwe() {
        assertThatThrownBy(() -> decrypter.decrypt("{\"username\":\"ana\"}"))
                .isInstanceOf(InvalidEncryptedPayloadException.class);
        verifyNoInteractions(validator);
    }

    @Test
    void propagaLosErroresDeValidacionDelContenidoDescifrado() throws Exception {
        String jwe = encrypt(encryptionKey.toPublicJWK(), JWEAlgorithm.RSA_OAEP_256, "{\"username\":\"ana\"}");
        when(validator.validate(any(LoginRequest.class))).thenReturn(Set.of(violation));

        assertThatThrownBy(() -> decrypter.decrypt(jwe)).isInstanceOf(ConstraintViolationException.class);
    }

    private static String encrypt(RSAKey publicKey, JWEAlgorithm alg, String json) throws Exception {
        JWEHeader header = new JWEHeader.Builder(alg, EncryptionMethod.A256GCM).keyID(publicKey.getKeyID()).build();
        JWEObject jwe = new JWEObject(header, new Payload(json));
        jwe.encrypt(new RSAEncrypter(publicKey));
        return jwe.serialize();
    }
}
