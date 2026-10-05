package ec.nexo.accounts.infrastructure.config;

import ec.nexo.accounts.application.port.out.NotificationPort;
import ec.nexo.accounts.infrastructure.adapter.out.notification.CustomerNotificationAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
class NotificationConfig {

    /** Hilos virtuales: cada aviso espera la red sin ocupar un hilo de plataforma. */
    @Bean(destroyMethod = "close")
    ExecutorService notificationExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    NotificationPort notificationPort(RestClient.Builder builder, ServicesProperties services,
                                      SecurityProperties security, ExecutorService notificationExecutor) {
        var requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(services.connectTimeout())
                .build());
        requestFactory.setReadTimeout(services.readTimeout());
        RestClient client = builder.clone()
                .baseUrl(services.customerUrl())
                .requestFactory(requestFactory)
                .defaultHeader("X-Internal-Api-Key", security.internalApiKey())
                .build();
        return new CustomerNotificationAdapter(client, notificationExecutor);
    }
}
