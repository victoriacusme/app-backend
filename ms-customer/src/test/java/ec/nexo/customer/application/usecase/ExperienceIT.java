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
@SpringBootTest(properties = "nexo.crypto.data-key=" + ExperienceIT.TEST_DATA_KEY)
@Testcontainers
class ExperienceIT {

    static final String TEST_DATA_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
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
    private DiscardCustomerUseCase discardCustomer;
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
    void descartarUnClienteBorraSusPreferenciasYEsIdempotente() {
        UUID id = UUID.randomUUID();
        provisionCustomer.execute(new NewCustomer(id, "Temporal", "1700000009", "t@nexo.ec", "+593990000009",
                LocalDate.of(1990, 1, 1)));

        discardCustomer.execute(id);
        discardCustomer.execute(id);

        assertThat(customers.findById(id)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM preferences WHERE customer_id = ?", Integer.class, id))
                .isZero();
    }

    @Test
    void laCedulaYElTelefonoQuedanCifradosEnLaBaseDeDatos() {
        UUID id = UUID.randomUUID();
        provisionCustomer.execute(new NewCustomer(id, "Pedro Ruiz", "1700000001", "pedro@nexo.ec", "+593990000001",
                LocalDate.of(1985, 3, 3)));

        Map<String, Object> row = jdbc.queryForMap("SELECT id_number, phone FROM customers WHERE id = ?", id);
        assertThat((String) row.get("id_number")).startsWith("v1:").doesNotContain("1700000001");
        assertThat((String) row.get("phone")).startsWith("v1:").doesNotContain("593990000001");

        Customer read = customers.findById(id).orElseThrow();
        assertThat(read.idNumber()).isEqualTo("1700000001");
        assertThat(read.phone()).isEqualTo("+593990000001");
    }

    @Test
    void laMigracionV101CifroLosDatosDeLaSemilla() {
        Integer plaintextRows = jdbc.queryForObject(
                "SELECT count(*) FROM customers WHERE id_number NOT LIKE 'v1:%' OR phone NOT LIKE 'v1:%'",
                Integer.class);

        assertThat(plaintextRows).isZero();
        assertThat(customers.findById(ANA).orElseThrow().idNumber()).isEqualTo("1712345678");
    }

    @Test
    void unClienteJovenNuevoNoRecibeLaMetaDeAhorroDeAna() {
        UUID id = UUID.randomUUID();
        provisionCustomer.execute(new NewCustomer(id, "Sofía Vera", "1700000003", "sofia@nexo.ec", "+593990000003",
                LocalDate.now().minusYears(20)));

        Experience home = getHome.execute(id);

        assertThat(home.segment()).isEqualTo(Segment.YOUNG);
        assertThat(home.components()).extracting(Experience.Component::type).doesNotContain("savings_goal");
        assertThat(types(ANA)).contains("savings_goal");
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
