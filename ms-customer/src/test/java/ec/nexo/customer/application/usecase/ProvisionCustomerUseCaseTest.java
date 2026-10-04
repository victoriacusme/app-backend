package ec.nexo.customer.application.usecase;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;
import ec.nexo.customer.application.port.out.DuplicateCustomerException;
import ec.nexo.customer.application.usecase.ProvisionCustomerUseCase.NewCustomer;
import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Preferences;
import ec.nexo.customer.domain.model.Segment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProvisionCustomerUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-04T15:00:00Z");

    @Mock
    private CustomerRepositoryPort customers;

    private ProvisionCustomerUseCase useCase;
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ProvisionCustomerUseCase(customers, Clock.fixed(NOW, ZoneOffset.UTC),
                ZoneId.of("America/Guayaquil"));
    }

    @Test
    void creaElClienteConSegmentoYPreferenciasPorDefecto() {
        when(customers.findById(customerId)).thenReturn(Optional.empty());

        var result = useCase.execute(command(LocalDate.of(2004, 1, 1)));

        assertThat(result.created()).isTrue();
        var customer = ArgumentCaptor.forClass(Customer.class);
        var preferences = ArgumentCaptor.forClass(Preferences.class);
        verify(customers).create(customer.capture(), preferences.capture());
        assertThat(customer.getValue().id()).isEqualTo(customerId);
        assertThat(customer.getValue().segment()).isEqualTo(Segment.YOUNG);
        assertThat(customer.getValue().createdAt()).isEqualTo(NOW);
        assertThat(preferences.getValue().customerId()).isEqualTo(customerId);
        assertThat(preferences.getValue().showPromotions()).isTrue();
    }

    @Test
    void esIdempotenteSiElClienteYaExiste() {
        Customer existing = customer();
        when(customers.findById(customerId)).thenReturn(Optional.of(existing));

        var result = useCase.execute(command(LocalDate.of(1990, 1, 1)));

        assertThat(result.created()).isFalse();
        assertThat(result.customer()).isSameAs(existing);
        verify(customers, never()).create(any(), any());
    }

    @Test
    void siOtraPeticionLoCreoAlMismoTiempoDevuelveEseCliente() {
        Customer createdByOther = customer();
        when(customers.findById(customerId)).thenReturn(Optional.empty()).thenReturn(Optional.of(createdByOther));
        doThrow(new DuplicateCustomerException(null)).when(customers).create(any(), any());

        var result = useCase.execute(command(LocalDate.of(1990, 1, 1)));

        assertThat(result.created()).isFalse();
        assertThat(result.customer()).isSameAs(createdByOther);
    }

    private NewCustomer command(LocalDate birthDate) {
        return new NewCustomer(customerId, "Pedro Ruiz", "1700000001", "pedro@nexo.ec", "+593990000001", birthDate);
    }

    private Customer customer() {
        return new Customer(customerId, "Pedro Ruiz", "1700000001", "pedro@nexo.ec", "+593990000001",
                LocalDate.of(1990, 1, 1), Segment.STANDARD, NOW);
    }
}
