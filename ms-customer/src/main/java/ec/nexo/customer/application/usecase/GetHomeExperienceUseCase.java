package ec.nexo.customer.application.usecase;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;
import ec.nexo.customer.application.port.out.ExperienceRepositoryPort;
import ec.nexo.customer.application.port.out.PreferencesRepositoryPort;
import ec.nexo.customer.domain.exception.CustomerNotFoundException;
import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Experience;
import ec.nexo.customer.domain.model.Preferences;
import ec.nexo.customer.domain.service.ExperienceComposer;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

/** Home server-driven: lo que ve el cliente depende de su segmento, la hora y sus preferencias. */
public class GetHomeExperienceUseCase {

    static final String HOME = "home";

    private final CustomerRepositoryPort customers;
    private final PreferencesRepositoryPort preferences;
    private final ExperienceRepositoryPort experiences;
    private final ExperienceComposer composer;
    private final Clock clock;
    private final ZoneId zone;

    public GetHomeExperienceUseCase(CustomerRepositoryPort customers, PreferencesRepositoryPort preferences,
                                    ExperienceRepositoryPort experiences, ExperienceComposer composer, Clock clock,
                                    ZoneId zone) {
        this.customers = customers;
        this.preferences = preferences;
        this.experiences = experiences;
        this.composer = composer;
        this.clock = clock;
        this.zone = zone;
    }

    @Transactional(readOnly = true)
    public Experience execute(UUID customerId) {
        Customer customer = customers.findById(customerId).orElseThrow(CustomerNotFoundException::new);
        Preferences prefs = preferences.findByCustomerId(customerId)
                .orElseGet(() -> Preferences.defaultsFor(customerId));
        return composer.compose(HOME, customer, prefs, experiences.findActiveByScreen(HOME),
                ZonedDateTime.now(clock.withZone(zone)));
    }
}
