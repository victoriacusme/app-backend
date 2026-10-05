package ec.nexo.auth.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9._]{3,30}$", message = "debe tener entre 3 y 30 letras, números, '.' o '_'")
        String username,
        @NotBlank
        @Size(min = 8, max = 128)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "debe contener al menos una letra y un número")
        String password,
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = "^\\d{10}$", message = "debe tener 10 dígitos") String idNumber,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Pattern(regexp = "^\\+?\\d{9,15}$", message = "no es un teléfono válido") String phone,
        @NotNull @Past LocalDate birthDate,
        @Size(max = 100) String deviceId) {

    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", deviceId=" + deviceId + "]";
    }
}
