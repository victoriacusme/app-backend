package ec.nexo.customer.infrastructure.adapter.in.web;

import ec.nexo.customer.application.usecase.RegisterDeviceUseCase;
import ec.nexo.customer.application.usecase.UnregisterDeviceUseCase;
import ec.nexo.customer.infrastructure.adapter.in.web.dto.RegisterDeviceRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registro del token de push del teléfono. PUT y DELETE son idempotentes: la app puede repetirlos sin miedo. */
@RestController
@RequestMapping("/customers/me/devices")
class DeviceController {

    private final RegisterDeviceUseCase registerDevice;
    private final UnregisterDeviceUseCase unregisterDevice;

    DeviceController(RegisterDeviceUseCase registerDevice, UnregisterDeviceUseCase unregisterDevice) {
        this.registerDevice = registerDevice;
        this.unregisterDevice = unregisterDevice;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RegisterDeviceRequest request) {
        registerDevice.execute(CurrentCustomer.id(jwt), request.token(), request.platform());
    }

    @DeleteMapping("/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unregister(@AuthenticationPrincipal Jwt jwt, @PathVariable String token) {
        unregisterDevice.execute(CurrentCustomer.id(jwt), token);
    }
}
