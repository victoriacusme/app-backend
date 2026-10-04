package ec.nexo.customer.infrastructure.adapter.in.web;

import ec.nexo.customer.application.usecase.DiscardCustomerUseCase;
import ec.nexo.customer.application.usecase.ProvisionCustomerUseCase;
import ec.nexo.customer.infrastructure.adapter.in.web.dto.InternalCustomerResponse;
import ec.nexo.customer.infrastructure.adapter.in.web.dto.NewCustomerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Endpoints servicio a servicio (API key). El gateway no los expone. */
@RestController
@RequestMapping("/internal/customers")
class InternalCustomerController {

    private final ProvisionCustomerUseCase provisionCustomer;
    private final DiscardCustomerUseCase discardCustomer;

    InternalCustomerController(ProvisionCustomerUseCase provisionCustomer, DiscardCustomerUseCase discardCustomer) {
        this.provisionCustomer = provisionCustomer;
        this.discardCustomer = discardCustomer;
    }

    /** 201 si se creó el cliente, 200 si ya existía (reintento idempotente de ms-auth). */
    @PostMapping
    ResponseEntity<InternalCustomerResponse> provision(@Valid @RequestBody NewCustomerRequest request) {
        var result = provisionCustomer.execute(request.toCommand());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(InternalCustomerResponse.from(result.customer()));
    }

    /** Compensación del onboarding. 204 exista o no el cliente (idempotente). */
    @DeleteMapping("/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void discard(@PathVariable UUID customerId) {
        discardCustomer.execute(customerId);
    }
}
