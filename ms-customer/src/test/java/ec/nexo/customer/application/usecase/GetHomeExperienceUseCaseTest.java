package ec.nexo.customer.application.usecase;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;
import ec.nexo.customer.application.port.out.ExperienceRepositoryPort;
import ec.nexo.customer.application.port.out.PreferencesRepositoryPort;
import ec.nexo.customer.domain.exception.CustomerNotFoundException;
import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Experience;
import ec.nexo.customer.domain.model.ExperienceComponent;
import ec.nexo.customer.domain.model.Segment;
import ec.nexo.customer.domain.service.ExperienceComposer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetHomeExperienceUseCaseTest {

    // 03:00 UTC = 22:00 del día anterior en Guayaquil (UTC-5): el saludo debe usar la hora local.
    private static final Instant NOW = Instant.parse("2026-10-05T03:00:00Z");

    @Mock
    private CustomerRepositoryPort customers;
    @Mock
    private PreferencesRepositoryPort preferences;
    @Mock
    private ExperienceRepositoryPort experiences;

    private GetHomeExperienceUseCase useCase;
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new GetHomeExperienceUseCase(customers, preferences, experiences, new ExperienceComposer(),
                Clock.fixed(NOW, ZoneOffset.UTC), ZoneId.of("America/Guayaquil"));
    }

    @Test
    void componeElHomeConLaHoraLocalDelCliente() {
        when(customers.findById(customerId)).thenReturn(Optional.of(new Customer(customerId, "Carlos Mena",
                "1709876543", "carlos@nexo.ec", "+593987654321", LocalDate.of(1979, 11, 2), Segment.PREMIUM,
                NOW)));
        when(preferences.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(experiences.findActiveByScreen("home")).thenReturn(List.of(new ExperienceComponent(UUID.randomUUID(),
                "greeting", "home", null, "greeting", 10, Map.of("title", "{greeting}, {firstName}"), true, false,
                null, null, null, null, null)));

        Experience home = useCase.execute(customerId);

        assertThat(home.segment()).isEqualTo(Segment.PREMIUM);
        assertThat(home.components().getFirst().props()).containsEntry("title", "Buenas noches, Carlos");
    }

    @Test
    void unClienteInexistenteDa404YLaAppUsaSuLayoutDeRespaldo() {
        when(customers.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(customerId)).isInstanceOf(CustomerNotFoundException.class);
        verifyNoInteractions(experiences);
    }
}
