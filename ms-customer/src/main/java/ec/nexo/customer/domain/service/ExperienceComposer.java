package ec.nexo.customer.domain.service;

import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Experience;
import ec.nexo.customer.domain.model.ExperienceComponent;
import ec.nexo.customer.domain.model.Preferences;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arma una pantalla server-driven para un cliente: filtra los componentes configurados por segmento, fechas,
 * franja horaria y preferencias, los ordena y personaliza sus textos ({greeting}, {firstName}).
 * No valida los {@code type}: la app ignora los que no conoce, así se pueden publicar tipos nuevos sin romper
 * versiones viejas.
 */
public class ExperienceComposer {

    public Experience compose(String screen, Customer customer, Preferences preferences,
                              List<ExperienceComponent> configured, ZonedDateTime now) {
        int localHour = now.getHour();
        Map<String, String> placeholders = Map.of(
                "{greeting}", greeting(localHour, preferences.language()),
                "{firstName}", customer.firstName());

        List<Experience.Component> components = configured.stream()
                .filter(c -> c.screen().equals(screen))
                .filter(c -> c.isVisibleTo(customer, preferences, now.toInstant(), localHour))
                .sorted(Comparator.comparingInt(ExperienceComponent::position))
                .map(c -> new Experience.Component(c.id(), c.type(), personalize(c.props(), placeholders)))
                .toList();
        return new Experience(screen, customer.segment(), components);
    }

    static String greeting(int hour, String language) {
        boolean english = "en".equals(language);
        if (hour >= 5 && hour < 12) {
            return english ? "Good morning" : "Buenos días";
        }
        if (hour >= 12 && hour < 19) {
            return english ? "Good afternoon" : "Buenas tardes";
        }
        return english ? "Good evening" : "Buenas noches";
    }

    /** Reemplaza los marcadores en los textos de primer nivel; el resto de propiedades viaja intacto. */
    private static Map<String, Object> personalize(Map<String, Object> props, Map<String, String> placeholders) {
        Map<String, Object> result = new LinkedHashMap<>(props);
        result.replaceAll((key, value) -> {
            if (!(value instanceof String text)) {
                return value;
            }
            for (var placeholder : placeholders.entrySet()) {
                text = text.replace(placeholder.getKey(), placeholder.getValue());
            }
            return text;
        });
        return result;
    }
}
