package ec.nexo.customer.domain.model;

import ec.nexo.customer.domain.exception.InvalidPreferenceException;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Preferencias del cliente. Afectan a la app (idioma, tema) y a la experiencia que se le compone (promociones). */
public class Preferences {

    public static final String DEFAULT_LANGUAGE = "es";
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("es", "en");

    private final UUID customerId;
    private String language;
    private Theme theme;
    private boolean notificationsEnabled;
    private boolean showPromotions;

    private Preferences(UUID customerId, String language, Theme theme, boolean notificationsEnabled,
                        boolean showPromotions) {
        this.customerId = Objects.requireNonNull(customerId);
        this.language = requireSupported(language);
        this.theme = Objects.requireNonNull(theme);
        this.notificationsEnabled = notificationsEnabled;
        this.showPromotions = showPromotions;
    }

    public static Preferences defaultsFor(UUID customerId) {
        return new Preferences(customerId, DEFAULT_LANGUAGE, Theme.SYSTEM, true, true);
    }

    public static Preferences restore(UUID customerId, String language, Theme theme, boolean notificationsEnabled,
                                      boolean showPromotions) {
        return new Preferences(customerId, language, theme, notificationsEnabled, showPromotions);
    }

    /** Aplica solo los campos informados (PATCH): un valor nulo deja el actual. */
    public void apply(Change change) {
        if (change.language() != null) {
            language = requireSupported(change.language());
        }
        if (change.theme() != null) {
            theme = change.theme();
        }
        if (change.notificationsEnabled() != null) {
            notificationsEnabled = change.notificationsEnabled();
        }
        if (change.showPromotions() != null) {
            showPromotions = change.showPromotions();
        }
    }

    private static String requireSupported(String language) {
        if (language == null || !SUPPORTED_LANGUAGES.contains(language)) {
            throw new InvalidPreferenceException("Idioma no soportado: " + language);
        }
        return language;
    }

    public UUID customerId() {
        return customerId;
    }

    public String language() {
        return language;
    }

    public Theme theme() {
        return theme;
    }

    public boolean notificationsEnabled() {
        return notificationsEnabled;
    }

    public boolean showPromotions() {
        return showPromotions;
    }

    public record Change(String language, Theme theme, Boolean notificationsEnabled, Boolean showPromotions) {
    }
}
