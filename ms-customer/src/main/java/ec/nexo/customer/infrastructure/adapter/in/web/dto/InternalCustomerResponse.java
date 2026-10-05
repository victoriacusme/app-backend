package ec.nexo.customer.infrastructure.adapter.in.web.dto;

import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Segment;

import java.util.UUID;

public record InternalCustomerResponse(UUID id, Segment segment) {

    public static InternalCustomerResponse from(Customer customer) {
        return new InternalCustomerResponse(customer.id(), customer.segment());
    }
}
