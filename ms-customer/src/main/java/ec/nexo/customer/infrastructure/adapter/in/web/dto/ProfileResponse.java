package ec.nexo.customer.infrastructure.adapter.in.web.dto;

import ec.nexo.customer.application.usecase.CustomerProfile;
import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Segment;

import java.util.UUID;

/** Cédula y teléfono salen enmascarados: la app solo los muestra, nunca los necesita completos. */
public record ProfileResponse(UUID id, String fullName, String firstName, String email, String phone,
                              String idNumber, Segment segment, PreferencesResponse preferences) {

    public static ProfileResponse from(CustomerProfile profile) {
        Customer customer = profile.customer();
        return new ProfileResponse(customer.id(), customer.fullName(), customer.firstName(), customer.email(),
                customer.maskedPhone(), customer.maskedIdNumber(), customer.segment(),
                PreferencesResponse.from(profile.preferences()));
    }
}
