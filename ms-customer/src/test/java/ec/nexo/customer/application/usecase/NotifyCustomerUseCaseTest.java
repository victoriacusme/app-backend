package ec.nexo.customer.application.usecase;

import ec.nexo.customer.application.port.out.CustomerRepositoryPort;
import ec.nexo.customer.application.port.out.DeviceTokenRepositoryPort;
import ec.nexo.customer.application.port.out.PreferencesRepositoryPort;
import ec.nexo.customer.application.port.out.PushSenderPort;
import ec.nexo.customer.application.port.out.PushSenderPort.SendResult;
import ec.nexo.customer.domain.exception.CustomerNotFoundException;
import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.DevicePlatform;
import ec.nexo.customer.domain.model.DeviceToken;
import ec.nexo.customer.domain.model.NotificationType;
import ec.nexo.customer.domain.model.Preferences;
import ec.nexo.customer.domain.model.PushMessage;
import ec.nexo.customer.domain.model.Segment;
import ec.nexo.customer.domain.model.Theme;
import ec.nexo.customer.domain.service.NotificationTemplates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyCustomerUseCaseTest {

    private static final Map<String, String> TRANSFER = Map.of("transferId", "t-1", "amount", "125.50",
            "currency", "USD", "targetAccount", "****7834");

    @Mock
    private CustomerRepositoryPort customers;
    @Mock
    private PreferencesRepositoryPort preferences;
    @Mock
    private DeviceTokenRepositoryPort devices;
    @Mock
    private PushSenderPort pushSender;

    private NotifyCustomerUseCase useCase;
    private final UUID ana = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new NotifyCustomerUseCase(customers, preferences, devices, pushSender, new NotificationTemplates());
    }

    @Test
    void enviaATodosLosDispositivosYBorraLosTokensInvalidos() {
        givenCustomer(true, "es");
        when(devices.findByCustomerId(ana)).thenReturn(List.of(device("telefono"), device("tablet"),
                device("desinstalada")));
        when(pushSender.send(eq("telefono"), any())).thenReturn(SendResult.DELIVERED);
        when(pushSender.send(eq("tablet"), any())).thenReturn(SendResult.FAILED);
        when(pushSender.send(eq("desinstalada"), any())).thenReturn(SendResult.INVALID_TOKEN);

        var result = useCase.execute(ana, NotificationType.TRANSFER_COMPLETED, TRANSFER);

        assertThat(result).isEqualTo(new NotifyCustomerUseCase.Result(1, 1, 1, null));
        verify(devices).deleteByToken("desinstalada");
        verify(devices, never()).deleteByToken("tablet");
    }

    @Test
    void usaElIdiomaDelCliente() {
        givenCustomer(true, "en");
        when(devices.findByCustomerId(ana)).thenReturn(List.of(device("telefono")));
        var message = ArgumentCaptor.forClass(PushMessage.class);
        when(pushSender.send(eq("telefono"), message.capture())).thenReturn(SendResult.DELIVERED);

        useCase.execute(ana, NotificationType.TRANSFER_COMPLETED, TRANSFER);

        assertThat(message.getValue().title()).isEqualTo("Transfer completed");
    }

    @Test
    void noEnviaNadaSiElClienteDesactivoLasNotificaciones() {
        givenCustomer(false, "es");

        var result = useCase.execute(ana, NotificationType.TRANSFER_COMPLETED, TRANSFER);

        assertThat(result.skipped()).isEqualTo(NotifyCustomerUseCase.Skipped.NOTIFICATIONS_DISABLED);
        verifyNoInteractions(devices, pushSender);
    }

    @Test
    void sinDispositivosRegistradosNoHayEnvio() {
        givenCustomer(true, "es");
        when(devices.findByCustomerId(ana)).thenReturn(List.of());

        assertThat(useCase.execute(ana, NotificationType.TRANSFER_COMPLETED, TRANSFER).skipped())
                .isEqualTo(NotifyCustomerUseCase.Skipped.NO_DEVICES);
        verifyNoInteractions(pushSender);
    }

    @Test
    void unClienteInexistenteEsUnError() {
        when(customers.findById(ana)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(ana, NotificationType.TRANSFER_COMPLETED, TRANSFER))
                .isInstanceOf(CustomerNotFoundException.class);
        verifyNoInteractions(pushSender);
    }

    private void givenCustomer(boolean notificationsEnabled, String language) {
        when(customers.findById(ana)).thenReturn(Optional.of(new Customer(ana, "Ana Torres", "1712345678",
                "ana@nexo.ec", "+593991234567", LocalDate.of(2003, 5, 14), Segment.YOUNG, Instant.now())));
        when(preferences.findByCustomerId(ana)).thenReturn(Optional.of(
                Preferences.restore(ana, language, Theme.SYSTEM, notificationsEnabled, true)));
    }

    private DeviceToken device(String token) {
        return new DeviceToken(token, ana, DevicePlatform.ANDROID);
    }
}
