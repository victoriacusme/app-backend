package ec.nexo.customer.infrastructure.adapter.in.web;

import ec.nexo.customer.application.usecase.GetMyProfileUseCase;
import ec.nexo.customer.application.usecase.UpdatePreferencesUseCase;
import ec.nexo.customer.infrastructure.adapter.in.web.dto.PreferencesPatchRequest;
import ec.nexo.customer.infrastructure.adapter.in.web.dto.PreferencesResponse;
import ec.nexo.customer.infrastructure.adapter.in.web.dto.ProfileResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers/me")
class CustomerController {

    private final GetMyProfileUseCase getMyProfile;
    private final UpdatePreferencesUseCase updatePreferences;

    CustomerController(GetMyProfileUseCase getMyProfile, UpdatePreferencesUseCase updatePreferences) {
        this.getMyProfile = getMyProfile;
        this.updatePreferences = updatePreferences;
    }

    @GetMapping
    ProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.from(getMyProfile.execute(CurrentCustomer.id(jwt)));
    }

    @PatchMapping("/preferences")
    PreferencesResponse updatePreferences(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody PreferencesPatchRequest request) {
        return PreferencesResponse.from(updatePreferences.execute(CurrentCustomer.id(jwt), request.toChange()));
    }
}
