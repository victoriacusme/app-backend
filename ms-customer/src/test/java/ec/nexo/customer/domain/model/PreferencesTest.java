package ec.nexo.customer.domain.model;

import ec.nexo.customer.domain.exception.InvalidPreferenceException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreferencesTest {

    @Test
    void soloCambiaLosCamposInformados() {
        Preferences preferences = Preferences.defaultsFor(UUID.randomUUID());

        preferences.apply(new Preferences.Change(null, Theme.DARK, null, false));

        assertThat(preferences.language()).isEqualTo("es");
        assertThat(preferences.theme()).isEqualTo(Theme.DARK);
        assertThat(preferences.notificationsEnabled()).isTrue();
        assertThat(preferences.showPromotions()).isFalse();
    }

    @Test
    void rechazaIdiomasNoSoportados() {
        Preferences preferences = Preferences.defaultsFor(UUID.randomUUID());

        assertThatThrownBy(() -> preferences.apply(new Preferences.Change("fr", null, null, null)))
                .isInstanceOf(InvalidPreferenceException.class);
        assertThat(preferences.language()).isEqualTo("es");
    }
}
