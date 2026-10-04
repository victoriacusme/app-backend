package ec.nexo.accounts.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** El monto llega como texto ("25.50"): así la app nunca pasa por un double y no hay errores de redondeo. */
public record TransferRequest(
        @NotNull UUID sourceAccountId,
        @NotNull UUID targetAccountId,
        @NotNull @Pattern(regexp = "\\d{1,13}(\\.\\d{1,2})?", message = "debe ser un monto con hasta dos decimales, p. ej. 25.50")
        String amount,
        @Size(max = 100) String description) {
}
