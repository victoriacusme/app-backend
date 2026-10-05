package ec.nexo.customer.application.usecase;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;
import ec.nexo.customer.application.port.out.DeviceTokenRepositoryPort;
import ec.nexo.customer.domain.exception.CustomerNotFoundException;
import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.DevicePlatform;
import ec.nexo.customer.domain.model.DeviceToken;
import ec.nexo.customer.domain.model.Segment;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceUseCasesTest {

    @Mock
    private CustomerRepositoryPort customers;
    @Mock
    private DeviceTokenRepositoryPort devices;

    private final UUID ana = UUID.randomUUID();

    @Test
    void registraElTokenDelCliente() {
        when(customers.findById(ana)).thenReturn(Optional.of(new Customer(ana, "Ana", "1712345678", "a@nexo.ec",
                "+593991234567", LocalDate.of(2003, 5, 14), Segment.YOUNG, Instant.now())));

        new RegisterDeviceUseCase(customers, devices).execute(ana, "fcm-token", DevicePlatform.IOS);

        verify(devices).register(new DeviceToken("fcm-token", ana, DevicePlatform.IOS));
    }

    @Test
    void noRegistraTokensDeClientesInexistentes() {
        when(customers.findById(ana)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new RegisterDeviceUseCase(customers, devices)
                .execute(ana, "fcm-token", DevicePlatform.IOS))
                .isInstanceOf(CustomerNotFoundException.class);
        verifyNoInteractions(devices);
    }

    @Test
    void soloBorraElTokenSiEsDelCliente() {
        new UnregisterDeviceUseCase(devices).execute(ana, "fcm-token");

        verify(devices).deleteByTokenAndCustomerId("fcm-token", ana);
    }

    @Test
    void elTokenNoSeImprimeCompleto() {
        assertThat(new DeviceToken("abcdefghijklmnopqrstuvwxyz-123456", ana, DevicePlatform.ANDROID).toString())
                .contains("…123456").doesNotContain("abcdefghij");
    }
}
