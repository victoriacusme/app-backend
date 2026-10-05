package ec.nexo.customer.infrastructure.adapter.in.web.dto;

import ec.nexo.customer.domain.model.Preferences;
import ec.nexo.customer.domain.model.Theme;
import jakarta.validation.constraints.Pattern;

/** PATCH parcial: solo se cambian los campos presentes. */
public record PreferencesPatchRequest(
        @Pattern(regexp = "es|en", message = "debe ser 'es' o 'en'") String language,
        Theme theme,
        Boolean notificationsEnabled,
        Boolean showPromotions) {

    public Preferences.Change toChange() {
        return new Preferences.Change(language, theme, notificationsEnabled, showPromotions);
    }
}
