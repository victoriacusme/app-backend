package ec.nexo.customer.infrastructure.adapter.in.web.dto;

import ec.nexo.customer.domain.model.NotificationType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/** Evento que otro servicio quiere notificar. Solo datos del evento: el texto lo arma ms-customer. */
public record NotificationRequest(@NotNull UUID customerId, @NotNull NotificationType type,
                                  Map<String, String> data) {
}
