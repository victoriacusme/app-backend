package ec.nexo.customer.application.usecase;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;
import ec.nexo.customer.application.port.out.PreferencesRepositoryPort;
import ec.nexo.customer.domain.exception.CustomerNotFoundException;
import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Preferences;
import ec.nexo.customer.domain.model.Segment;
import ec.nexo.customer.domain.model.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreferencesAndProfileUseCasesTest {

    @Mock
    private CustomerRepositoryPort customers;
    @Mock
    private PreferencesRepositoryPort preferences;

    private final UUID customerId = UUID.randomUUID();

    @Test
    void elPerfilUsaPreferenciasPorDefectoSiNoHayGuardadas() {
        when(customers.findById(customerId)).thenReturn(Optional.of(customer()));
        when(preferences.findByCustomerId(customerId)).thenReturn(Optional.empty());

        CustomerProfile profile = new GetMyProfileUseCase(customers, preferences).execute(customerId);

        assertThat(profile.customer().id()).isEqualTo(customerId);
        assertThat(profile.preferences().language()).isEqualTo("es");
    }

    @Test
    void actualizaYGuardaLasPreferencias() {
        when(customers.findById(customerId)).thenReturn(Optional.of(customer()));
        when(preferences.findByCustomerId(customerId))
                .thenReturn(Optional.of(Preferences.restore(customerId, "es", Theme.LIGHT, true, true)));
        when(preferences.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Preferences updated = new UpdatePreferencesUseCase(customers, preferences)
                .execute(customerId, new Preferences.Change("en", null, null, false));

        assertThat(updated.language()).isEqualTo("en");
        assertThat(updated.theme()).isEqualTo(Theme.LIGHT);
        assertThat(updated.showPromotions()).isFalse();
        verify(preferences).save(updated);
    }

    @Test
    void noActualizaPreferenciasDeUnClienteInexistente() {
        when(customers.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new UpdatePreferencesUseCase(customers, preferences)
                .execute(customerId, new Preferences.Change("en", null, null, null)))
                .isInstanceOf(CustomerNotFoundException.class);
        verifyNoInteractions(preferences);
    }

    private Customer customer() {
        return new Customer(customerId, "Ana Torres", "1712345678", "ana@nexo.ec", "+593991234567",
                LocalDate.of(2003, 5, 14), Segment.YOUNG, Instant.now());
    }
}
