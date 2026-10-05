package ec.nexo.accounts.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OpenAccountRequest(@NotNull UUID customerId) {
}
