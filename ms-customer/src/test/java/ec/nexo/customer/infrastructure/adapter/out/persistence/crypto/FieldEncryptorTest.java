package ec.nexo.customer.infrastructure.adapter.out.persistence.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldEncryptorTest {

    private final FieldEncryptor encryptor = new FieldEncryptor(randomKey());

    @Test
    void cifraYDescifraSinPerderInformacion() {
        String stored = encryptor.encrypt("1712345678");

        assertThat(stored).startsWith("v1:").doesNotContain("1712345678");
        assertThat(encryptor.decrypt(stored)).isEqualTo("1712345678");
    }

    @Test
    void elMismoValorProduceTextosCifradosDistintos() {
        // IV aleatorio: en la BD no se puede saber si dos clientes comparten cédula o teléfono.
        assertThat(encryptor.encrypt("+593991234567")).isNotEqualTo(encryptor.encrypt("+593991234567"));
    }

    @Test
    void detectaUnValorAlteradoEnLaBaseDeDatos() {
        String stored = encryptor.encrypt("1712345678");
        byte[] payload = Base64.getDecoder().decode(stored.substring(3));
        payload[payload.length - 1] ^= 1;
        String tampered = "v1:" + Base64.getEncoder().encodeToString(payload);

        assertThatThrownBy(() -> encryptor.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("alterado");
    }

    @Test
    void otraClaveNoPuedeDescifrar() {
        String stored = encryptor.encrypt("1712345678");

        assertThatThrownBy(() -> new FieldEncryptor(randomKey()).decrypt(stored))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void losValoresEnClaroAnterioresAlCifradoSeLeenTalCual() {
        assertThat(encryptor.isEncrypted("1712345678")).isFalse();
        assertThat(encryptor.decrypt("1712345678")).isEqualTo("1712345678");
        assertThat(encryptor.encrypt(null)).isNull();
        assertThat(encryptor.decrypt(null)).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"no-es-base64!!", "c2hvcnQ="})
    void sinUnaClaveAes256ValidaNoArranca(String key) {
        assertThatThrownBy(() -> new FieldEncryptor(key))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("openssl rand -base64 32");
    }

    private static String randomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
