package ec.nexo.customer.infrastructure.config;

import com.google.firebase.FirebaseApp;
import ec.nexo.customer.infrastructure.adapter.out.push.FcmPushSender;
import ec.nexo.customer.infrastructure.adapter.out.push.LogPushSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class PushConfigTest {

    @TempDir
    Path tmp;

    @AfterEach
    void deleteFirebaseApps() {
        FirebaseApp.getApps().forEach(FirebaseApp::delete);
    }

    @Test
    void sinCredencialesLasPushVanAlLog() throws Exception {
        assertThat(new PushConfig().pushSender(new PushConfig.PushProperties("")))
                .isInstanceOf(LogPushSender.class);
    }

    /** Regresión: con credenciales, FirebaseOptions necesita JacksonFactory en el classpath. */
    @Test
    void conCuentaDeServicioUsaFcm() throws Exception {
        Path credentials = tmp.resolve("firebase-adminsdk.json");
        Files.writeString(credentials, fakeServiceAccount());

        assertThat(new PushConfig().pushSender(new PushConfig.PushProperties(credentials.toString())))
                .isInstanceOf(FcmPushSender.class);
    }

    /** Cuenta de servicio con una clave RSA generada al vuelo: no toca la red ni un proyecto real. */
    private static String fakeServiceAccount() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String pem = "-----BEGIN PRIVATE KEY-----\\n"
                + Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
                + "\\n-----END PRIVATE KEY-----\\n";
        return """
                {
                  "type": "service_account",
                  "project_id": "nexo-test",
                  "private_key_id": "test-key",
                  "private_key": "%s",
                  "client_email": "push@nexo-test.iam.gserviceaccount.com",
                  "client_id": "1",
                  "token_uri": "https://oauth2.googleapis.com/token"
                }
                """.formatted(pem);
    }
}
