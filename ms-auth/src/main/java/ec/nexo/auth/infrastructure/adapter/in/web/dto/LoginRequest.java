package ec.nexo.auth.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 30) String username,
        @NotBlank @Size(max = 128) String password,
        @Size(max = 100) String deviceId) {

    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", deviceId=" + deviceId + "]";
    }
}
