package ec.nexo.customer.infrastructure.adapter.in.web.dto;

import ec.nexo.customer.domain.model.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(@NotBlank @Size(max = 512) String token, @NotNull DevicePlatform platform) {

    @Override
    public String toString() {
        return "RegisterDeviceRequest[platform=" + platform + "]";
    }
}
