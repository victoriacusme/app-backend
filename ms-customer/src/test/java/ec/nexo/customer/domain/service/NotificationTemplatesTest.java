package ec.nexo.customer.domain.service;

import ec.nexo.customer.domain.exception.InvalidNotificationException;
import ec.nexo.customer.domain.model.NotificationType;
import ec.nexo.customer.domain.model.PushMessage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTemplatesTest {

    private static final Map<String, String> TRANSFER = Map.of("transferId", "t-1", "amount", "125.50",
            "currency", "USD", "targetAccount", "****7834");

    private final NotificationTemplates templates = new NotificationTemplates();

    @Test
    void armaElAvisoDeTransferenciaEnEspanolConDeepLink() {
        PushMessage message = templates.render(NotificationType.TRANSFER_COMPLETED, TRANSFER, "es");

        assertThat(message.title()).isEqualTo("Transferencia exitosa");
        assertThat(message.body()).isEqualTo("Transferiste $125.50 a tu cuenta ****7834");
        assertThat(message.data()).containsEntry("deeplink", "app://transfers/t-1")
                .containsEntry("type", "TRANSFER_COMPLETED");
    }

    @Test
    void respetaElIdiomaDelCliente() {
        PushMessage message = templates.render(NotificationType.TRANSFER_COMPLETED, TRANSFER, "en");

        assertThat(message.title()).isEqualTo("Transfer completed");
        assertThat(message.body()).isEqualTo("You transferred $125.50 to your account ****7834");
    }

    @Test
    void otrasMonedasSeMuestranConSuCodigo() {
        var inEuros = Map.of("transferId", "t-1", "amount", "10.00", "currency", "EUR", "targetAccount", "****1");

        assertThat(templates.render(NotificationType.TRANSFER_COMPLETED, inEuros, "es").body()).contains("10.00 EUR");
    }

    @Test
    void unEventoIncompletoEsUnErrorDelEmisor() {
        assertThatThrownBy(() -> templates.render(NotificationType.TRANSFER_COMPLETED, Map.of("amount", "1"), "es"))
                .isInstanceOf(InvalidNotificationException.class)
                .hasMessageContaining("transferId");
    }
}
