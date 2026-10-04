package ec.nexo.customer.infrastructure.adapter.in.web;

import ec.nexo.customer.application.usecase.ProvisionCustomerUseCase;
import ec.nexo.customer.infrastructure.adapter.in.web.dto.InternalCustomerResponse;
import ec.nexo.customer.infrastructure.adapter.in.web.dto.NewCustomerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints servicio a servicio (API key). El gateway no los expone. */
@RestController
@RequestMapping("/internal/customers")
class InternalCustomerController {

    private final ProvisionCustomerUseCase provisionCustomer;

    InternalCustomerController(ProvisionCustomerUseCase provisionCustomer) {
        this.provisionCustomer = provisionCustomer;
    }

    /** 201 si se creó el cliente, 200 si ya existía (reintento idempotente de ms-auth). */
    @PostMapping
    ResponseEntity<InternalCustomerResponse> provision(@Valid @RequestBody NewCustomerRequest request) {
        var result = provisionCustomer.execute(request.toCommand());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(InternalCustomerResponse.from(result.customer()));
    }
}
