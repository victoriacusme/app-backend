package ec.nexo.auth.infrastructure.adapter.out.http;

import ec.nexo.auth.application.port.out.AccountProvisioningPort;
import ec.nexo.auth.application.port.out.ProvisioningFailedException;
import ec.nexo.auth.infrastructure.config.ServicesProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
class AccountHttpAdapter implements AccountProvisioningPort {

    private final RestClient client;

    AccountHttpAdapter(InternalRestClientFactory factory, ServicesProperties properties) {
        this.client = factory.create(properties.accountsUrl());
    }

    @Override
    public void openDefaultAccount(UUID customerId) {
        try {
            client.post()
                    .uri("/internal/accounts")
                    .body(new OpenAccountRequest(customerId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ProvisioningFailedException("ms-accounts", e);
        }
    }

    record OpenAccountRequest(UUID customerId) {
    }
}
