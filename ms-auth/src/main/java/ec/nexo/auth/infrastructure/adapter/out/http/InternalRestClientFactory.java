package ec.nexo.auth.infrastructure.adapter.out.http;

import ec.nexo.auth.infrastructure.config.ServicesProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/** Construye clientes HTTP hacia los endpoints {@code /internal/**} con API key y timeouts. */
@Component
class InternalRestClientFactory {

    static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient.Builder builder;
    private final ServicesProperties properties;

    InternalRestClientFactory(RestClient.Builder builder, ServicesProperties properties) {
        this.builder = builder;
        this.properties = properties;
    }

    RestClient create(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(API_KEY_HEADER, properties.internalApiKey())
                .build();
    }
}
