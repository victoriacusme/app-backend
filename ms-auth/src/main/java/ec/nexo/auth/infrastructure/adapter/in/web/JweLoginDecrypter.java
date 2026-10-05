package ec.nexo.auth.infrastructure.adapter.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import ec.nexo.auth.infrastructure.adapter.in.web.dto.LoginRequest;
import ec.nexo.auth.infrastructure.adapter.out.security.RsaKeys;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.text.ParseException;

/**
 * Descifra el cuerpo de un login enviado como JWE compacto ({@code Content-Type: application/jose}).
 * Solo acepta RSA-OAEP-256 + A256GCM con la clave {@code enc} publicada en el JWKS.
 */
@Component
class JweLoginDecrypter {

    private final RsaKeys keys;
    private final RSADecrypter decrypter;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    JweLoginDecrypter(RsaKeys keys, ObjectMapper objectMapper, Validator validator) throws JOSEException {
        this.keys = keys;
        this.decrypter = new RSADecrypter(keys.encryptionKey());
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    LoginRequest decrypt(String compactJwe) {
        JWEObject jwe;
        try {
            jwe = JWEObject.parse(compactJwe.trim());
        } catch (ParseException e) {
            throw new InvalidEncryptedPayloadException("El cuerpo no es un JWE compacto válido");
        }

        var header = jwe.getHeader();
        if (!JWEAlgorithm.RSA_OAEP_256.equals(header.getAlgorithm())
                || !EncryptionMethod.A256GCM.equals(header.getEncryptionMethod())) {
            throw new InvalidEncryptedPayloadException("Solo se acepta alg=RSA-OAEP-256 y enc=A256GCM");
        }
        String kid = header.getKeyID();
        if (kid != null && !kid.equals(keys.encryptionKey().getKeyID())) {
            throw new InvalidEncryptedPayloadException("La clave de cifrado (kid) no es la vigente");
        }

        LoginRequest request;
        try {
            jwe.decrypt(decrypter);
            request = objectMapper.readValue(jwe.getPayload().toString(), LoginRequest.class);
        } catch (JOSEException e) {
            throw new InvalidEncryptedPayloadException("No se pudo descifrar el JWE");
        } catch (JsonProcessingException e) {
            throw new InvalidEncryptedPayloadException("El contenido del JWE no es un login válido");
        }

        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return request;
    }
}
