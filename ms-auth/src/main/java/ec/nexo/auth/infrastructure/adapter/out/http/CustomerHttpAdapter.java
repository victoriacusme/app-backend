package ec.nexo.auth.infrastructure.adapter.out.http;

import ec.nexo.auth.application.port.out.CustomerProvisioningPort;
import ec.nexo.auth.application.port.out.ProvisioningFailedException;
import ec.nexo.auth.infrastructure.config.ServicesProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class CustomerHttpAdapter implements CustomerProvisioningPort {

    private final RestClient client;

    CustomerHttpAdapter(InternalRestClientFactory factory, ServicesProperties properties) {
        this.client = factory.create(properties.customerUrl());
    }

    @Override
    public void provision(NewCustomer customer) {
        try {
            client.post()
                    .uri("/internal/customers")
                    .body(customer)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ProvisioningFailedException("ms-customer", e);
        }
    }
}
