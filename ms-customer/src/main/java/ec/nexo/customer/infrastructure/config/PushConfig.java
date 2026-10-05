package ec.nexo.customer.infrastructure.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import ec.nexo.customer.application.port.out.PushSenderPort;
import ec.nexo.customer.infrastructure.adapter.out.push.FcmPushSender;
import ec.nexo.customer.infrastructure.adapter.out.push.LogPushSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * FCM si hay credenciales de Firebase (cuenta de servicio en JSON); si no, push simulado en el log.
 * Así la demo funciona sin un proyecto de Firebase y producción solo necesita montar el archivo.
 */
@Configuration
class PushConfig {

    private static final Logger log = LoggerFactory.getLogger(PushConfig.class);

    @Bean
    PushSenderPort pushSender(PushProperties properties) throws IOException {
        String credentialsFile = properties.fcmCredentialsFile();
        if (credentialsFile == null || credentialsFile.isBlank()) {
            log.info("FCM_CREDENTIALS_FILE no está configurado: las notificaciones push se registran en el log");
            return new LogPushSender();
        }
        try (InputStream credentials = new FileInputStream(credentialsFile)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            log.info("Notificaciones push vía Firebase Cloud Messaging");
            return new FcmPushSender(FirebaseMessaging.getInstance(app));
        }
    }

    @ConfigurationProperties("nexo.push")
    record PushProperties(String fcmCredentialsFile) {
    }
}
