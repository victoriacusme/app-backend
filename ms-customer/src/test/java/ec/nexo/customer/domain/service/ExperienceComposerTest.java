package ec.nexo.customer.domain.service;

import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Experience;
import ec.nexo.customer.domain.model.ExperienceComponent;
import ec.nexo.customer.domain.model.Preferences;
import ec.nexo.customer.domain.model.Segment;
import ec.nexo.customer.domain.model.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExperienceComposerTest {

    private static final ZoneId GUAYAQUIL = ZoneId.of("America/Guayaquil");
    private static final ZonedDateTime AFTERNOON = ZonedDateTime.of(2026, 10, 4, 15, 0, 0, 0, GUAYAQUIL);

    private final ExperienceComposer composer = new ExperienceComposer();

    private final List<ExperienceComponent> configured = List.of(
            component("greeting", null, "greeting", 10, Map.of("title", "{greeting}, {firstName}")),
            component("summary", null, "accounts_summary", 20, Map.of("title", "Tus cuentas")),
            component("young-goal", Segment.YOUNG, "savings_goal", 40, Map.of("target", "1000.00")),
            component("premium-fx", Segment.PREMIUM, "fx_rates", 40, Map.of("base", "USD")),
            component("entrepreneur-chart", Segment.ENTREPRENEUR, "cash_flow_chart", 50, Map.of()));

    @Test
    void cadaSegmentoRecibeSuPropioHomeSobreLaBaseComun() {
        assertThat(types(compose(customer(Segment.YOUNG), prefs(true), configured)))
                .containsExactly("greeting", "accounts_summary", "savings_goal");
        assertThat(types(compose(customer(Segment.PREMIUM), prefs(true), configured)))
                .containsExactly("greeting", "accounts_summary", "fx_rates");
        assertThat(types(compose(customer(Segment.ENTREPRENEUR), prefs(true), configured)))
                .containsExactly("greeting", "accounts_summary", "cash_flow_chart");
    }

    @Test
    void ordenaPorPosicionSinImportarElOrdenDeCarga() {
        var reversed = configured.reversed();

        assertThat(types(compose(customer(Segment.YOUNG), prefs(true), reversed)))
                .containsExactly("greeting", "accounts_summary", "savings_goal");
    }

    @Test
    void personalizaElSaludoConElNombreYLaHora() {
        Experience home = compose(customer(Segment.YOUNG), prefs(true), configured);

        assertThat(home.components().getFirst().props()).containsEntry("title", "Buenas tardes, Ana");
        assertThat(home.segment()).isEqualTo(Segment.YOUNG);
    }

    @ParameterizedTest
    @CsvSource({"6,es,Buenos días", "11,es,Buenos días", "12,es,Buenas tardes", "19,es,Buenas noches",
            "2,es,Buenas noches", "8,en,Good morning", "15,en,Good afternoon", "22,en,Good evening"})
    void elSaludoDependeDeLaHoraLocalYDelIdioma(int hour, String language, String expected) {
        assertThat(ExperienceComposer.greeting(hour, language)).isEqualTo(expected);
    }

    @Test
    void siElClienteDesactivaLasPromocionesNoVeBanners() {
        var withPromo = List.of(
                component("greeting", null, "greeting", 10, Map.of()),
                promotion("promo", null, 50));

        assertThat(types(compose(customer(Segment.YOUNG), prefs(true), withPromo)))
                .containsExactly("greeting", "promo_banner");
        assertThat(types(compose(customer(Segment.YOUNG), prefs(false), withPromo)))
                .containsExactly("greeting");
    }

    @Test
    void respetaLaVentanaDeUnaCampana() {
        Instant now = AFTERNOON.toInstant();
        var campaigns = List.of(
                campaign("vigente", now.minusSeconds(60), now.plusSeconds(60)),
                campaign("futura", now.plusSeconds(60), null),
                campaign("vencida", null, now));

        assertThat(compose(customer(Segment.YOUNG), prefs(true), campaigns).components())
                .extracting(c -> c.props().get("name"))
                .containsExactly("vigente");
    }

    @Test
    void respetaLaFranjaHorariaLocal() {
        var lunch = new ExperienceComponent(UUID.randomUUID(), "almuerzo", "home", null, "promo_banner", 10,
                Map.of(), true, false, null, null, 12, 15);

        assertThat(compose(customer(Segment.YOUNG), prefs(true), List.of(lunch)).components()).isEmpty();
        assertThat(composer.compose("home", customer(Segment.YOUNG), prefs(true), List.of(lunch),
                AFTERNOON.withHour(13)).components()).hasSize(1);
    }

    @Test
    void ignoraLosComponentesInactivosYLosDeOtraPantalla() {
        var others = List.of(
                new ExperienceComponent(UUID.randomUUID(), "apagado", "home", null, "promo_banner", 10, Map.of(),
                        false, false, null, null, null, null),
                new ExperienceComponent(UUID.randomUUID(), "perfil", "profile", null, "banner", 10, Map.of(),
                        true, false, null, null, null, null));

        assertThat(compose(customer(Segment.YOUNG), prefs(true), others).components()).isEmpty();
    }

    @Test
    void dejaPasarTiposDesconocidosYPropiedadesAnidadasSinTocarlas() {
        Map<String, Object> nested = Map.of("actions", List.of(Map.of("label", "{firstName}")), "count", 3);
        var unknown = List.of(component("nuevo", null, "tipo_que_la_app_no_conoce", 10, nested));

        Experience home = compose(customer(Segment.YOUNG), prefs(true), unknown);

        assertThat(home.components().getFirst().type()).isEqualTo("tipo_que_la_app_no_conoce");
        assertThat(home.components().getFirst().props()).isEqualTo(nested);
    }

    private Experience compose(Customer customer, Preferences preferences, List<ExperienceComponent> components) {
        return composer.compose("home", customer, preferences, components, AFTERNOON);
    }

    private static List<String> types(Experience experience) {
        return experience.components().stream().map(Experience.Component::type).toList();
    }

    private static Customer customer(Segment segment) {
        return new Customer(UUID.randomUUID(), "Ana Torres", "1712345678", "ana@nexo.ec", "+593991234567",
                LocalDate.of(2003, 5, 14), segment, Instant.now());
    }

    private static Preferences prefs(boolean showPromotions) {
        return Preferences.restore(UUID.randomUUID(), "es", Theme.SYSTEM, true, showPromotions);
    }

    private static ExperienceComponent component(String name, Segment segment, String type, int position,
                                                 Map<String, Object> props) {
        return new ExperienceComponent(UUID.randomUUID(), name, "home", segment, type, position, props, true, false,
                null, null, null, null);
    }

    private static ExperienceComponent promotion(String name, Segment segment, int position) {
        return new ExperienceComponent(UUID.randomUUID(), name, "home", segment, "promo_banner", position, Map.of(),
                true, true, null, null, null, null);
    }

    private static ExperienceComponent campaign(String name, Instant startsAt, Instant endsAt) {
        return new ExperienceComponent(UUID.randomUUID(), name, "home", null, "promo_banner", 10,
                Map.of("name", name), true, false, startsAt, endsAt, null, null);
    }
}
