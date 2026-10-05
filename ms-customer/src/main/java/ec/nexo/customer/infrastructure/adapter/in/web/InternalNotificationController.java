package ec.nexo.customer.infrastructure.adapter.in.web;

import ec.nexo.customer.application.usecase.NotifyCustomerUseCase;
import ec.nexo.customer.infrastructure.adapter.in.web.dto.NotificationRequest;
import ec.nexo.customer.infrastructure.adapter.in.web.dto.NotificationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Otros servicios piden notificar a un cliente (p. ej. ms-accounts tras una transferencia). API key; no expuesto. */
@RestController
@RequestMapping("/internal/notifications")
class InternalNotificationController {

    private final NotifyCustomerUseCase notifyCustomer;

    InternalNotificationController(NotifyCustomerUseCase notifyCustomer) {
        this.notifyCustomer = notifyCustomer;
    }

    @PostMapping
    NotificationResponse notify(@Valid @RequestBody NotificationRequest request) {
        return NotificationResponse.from(notifyCustomer.execute(request.customerId(), request.type(),
                request.data()));
    }
}
