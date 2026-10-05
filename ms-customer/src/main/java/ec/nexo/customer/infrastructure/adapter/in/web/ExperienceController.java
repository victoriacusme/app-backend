package ec.nexo.customer.infrastructure.adapter.in.web;

import ec.nexo.customer.application.usecase.GetHomeExperienceUseCase;
import ec.nexo.customer.infrastructure.adapter.in.web.dto.ExperienceResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/experience")
class ExperienceController {

    private final GetHomeExperienceUseCase getHomeExperience;

    ExperienceController(GetHomeExperienceUseCase getHomeExperience) {
        this.getHomeExperience = getHomeExperience;
    }

    /** "no-cache" no impide guardar: obliga a revalidar con el ETag (304 si no cambió). Es privado por cliente. */
    @GetMapping("/home")
    ResponseEntity<ExperienceResponse> home(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(ExperienceResponse.from(getHomeExperience.execute(CurrentCustomer.id(jwt))));
    }
}
