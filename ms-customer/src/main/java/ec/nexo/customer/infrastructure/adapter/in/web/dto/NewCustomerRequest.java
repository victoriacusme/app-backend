package ec.nexo.customer.infrastructure.adapter.in.web.dto;

import ec.nexo.customer.application.usecase.ProvisionCustomerUseCase.NewCustomer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/** Mismas reglas que valida ms-auth en el registro: este servicio no confía en que ya vengan validados. */
public record NewCustomerRequest(
        @NotNull UUID customerId,
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = "^\\d{10}$", message = "debe tener 10 dígitos") String idNumber,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Pattern(regexp = "^\\+?\\d{9,15}$", message = "no es un teléfono válido") String phone,
        @NotNull @Past LocalDate birthDate) {

    public NewCustomer toCommand() {
        return new NewCustomer(customerId, fullName, idNumber, email, phone, birthDate);
    }

    /** Evita que los datos personales terminen en logs si alguien registra la petición completa. */
    @Override
    public String toString() {
        return "NewCustomerRequest[customerId=" + customerId + "]";
    }
}
