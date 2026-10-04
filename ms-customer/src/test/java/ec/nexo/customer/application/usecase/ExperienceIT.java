package ec.nexo.customer.application.usecase;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;
import ec.nexo.customer.application.port.out.DuplicateCustomerException;
import ec.nexo.customer.application.usecase.ProvisionCustomerUseCase.NewCustomer;
import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Experience;
import ec.nexo.customer.domain.model.Preferences;
import ec.nexo.customer.domain.model.Segment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Experiencia server-driven completa contra PostgreSQL real: semilla, JSONB, composer y cambios en vivo en BD. */
@SpringBootTest
@Testcontainers
class ExperienceIT {

    private static final UUID ANA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CARLOS = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID LUCIA = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private GetHomeExperienceUseCase getHome;
    @Autowired
    private UpdatePreferencesUseCase updatePreferences;
    @Autowired
    private ProvisionCustomerUseCase provisionCustomer;
    @Autowired
    private CustomerRepositoryPort customers;
    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void restoreSeed() {
        jdbc.update("UPDATE experience_components SET active = FALSE WHERE name = 'campaign-black-friday'");
        jdbc.update("UPDATE preferences SET show_promotions = TRUE, language = 'es'");
    }

    @Test
    void losTresClientesDeLaSemillaVenTresHomesDistintos() {
        assertThat(types(ANA)).containsExactly("greeting", "accounts_summary", "quick_actions", "savings_goal",
                "promo_banner");
        assertThat(types(CARLOS)).containsExactly("greeting", "accounts_summary", "quick_actions", "fx_rates",
                "promo_banner");
        assertThat(types(LUCIA)).containsExactly("greeting", "accounts_summary", "quick_actions", "promo_banner",
                "cash_flow_chart");
    }

    @Test
    void lasPropiedadesJsonbLleganComoEstructurasYConElNombrePersonalizado() {
        Experience home = getHome.execute(CARLOS);

        assertThat((String) home.components().getFirst().props().get("title")).endsWith(", Carlos");
        Map<String, Object> fx = component(home, "fx_rates").props();
        assertThat(fx.get("symbols")).isEqualTo(List.of("EUR", "COP", "PEN", "MXN"));
        @SuppressWarnings("unchecked")
        var actions = (List<Map<String, Object>>) component(home, "quick_actions").props().get("actions");
        assertThat(actions).extracting(a -> a.get("deeplink")).contains("app://investments");
    }

    @Test
    void activarUnaFilaEnBdCambiaElHomeDeTodosSinPublicarNada() {
        assertThat(types(ANA)).hasSize(5);

        jdbc.update("UPDATE experience_components SET active = TRUE WHERE name = 'campaign-black-friday'");

        for (UUID customer : List.of(ANA, CARLOS, LUCIA)) {
            Experience home = getHome.execute(customer);
            // Posición 15: justo después del saludo.
            assertThat(home.components().get(1).props()).containsEntry("title", "Black Friday Nexo");
        }
    }

    @Test
    void desactivarLasPromocionesQuitaLosBannersDelHome() {
        updatePreferences.execute(ANA, new Preferences.Change(null, null, null, false));

        assertThat(types(ANA)).doesNotContain("promo_banner");
        assertThat(types(CARLOS)).contains("promo_banner");
    }

    @Test
    void elAltaDeUnClienteNuevoEsIdempotenteYRecibeElHomeEstandar() {
        UUID id = UUID.randomUUID();
        var command = new NewCustomer(id, "Pedro Ruiz", "1700000001", "Pedro@Nexo.ec", "+593990000001",
                LocalDate.of(1985, 3, 3));

        assertThat(provisionCustomer.execute(command).created()).isTrue();
        assertThat(provisionCustomer.execute(command).created()).isFalse();

        Experience home = getHome.execute(id);
        assertThat(home.segment()).isEqualTo(Segment.STANDARD);
        assertThat(home.components()).extracting(Experience.Component::type)
                .containsExactly("greeting", "accounts_summary", "quick_actions");
        assertThat(jdbc.queryForObject("SELECT email FROM customers WHERE id = ?", String.class, id))
                .isEqualTo("pedro@nexo.ec");
    }

    @Test
    void elRepositorioDetectaUnAltaDuplicada() {
        Customer existing = new Customer(ANA, "Ana", "1712345678", "ana@nexo.ec", "+593991234567",
                LocalDate.of(2003, 5, 14), Segment.YOUNG, Instant.now());

        assertThatThrownBy(() -> customers.create(existing, Preferences.defaultsFor(ANA)))
                .isInstanceOf(DuplicateCustomerException.class);
    }

    private List<String> types(UUID customerId) {
        return getHome.execute(customerId).components().stream().map(Experience.Component::type).toList();
    }

    private static Experience.Component component(Experience experience, String type) {
        return experience.components().stream().filter(c -> c.type().equals(type)).findFirst().orElseThrow();
    }
}
