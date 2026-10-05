package ec.nexo.customer.infrastructure.adapter.in.web.dto;

import ec.nexo.customer.domain.model.Preferences;
import ec.nexo.customer.domain.model.Theme;

public record PreferencesResponse(String language, Theme theme, boolean notificationsEnabled,
                                  boolean showPromotions) {

    public static PreferencesResponse from(Preferences preferences) {
        return new PreferencesResponse(preferences.language(), preferences.theme(),
                preferences.notificationsEnabled(), preferences.showPromotions());
    }
}
