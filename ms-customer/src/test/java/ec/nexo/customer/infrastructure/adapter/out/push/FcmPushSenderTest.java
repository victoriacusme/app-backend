package ec.nexo.customer.infrastructure.adapter.out.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import ec.nexo.customer.application.port.out.PushSenderPort.SendResult;
import ec.nexo.customer.domain.model.PushMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmPushSenderTest {

    private static final PushMessage MESSAGE = new PushMessage("Transferencia exitosa", "Transferiste $10.00",
            Map.of("deeplink", "app://transfers/t-1"));

    @Mock
    private FirebaseMessaging messaging;

    @Test
    void entregaElMensajeAFcm() throws Exception {
        when(messaging.send(any(Message.class))).thenReturn("projects/nexo/messages/1");

        assertThat(new FcmPushSender(messaging).send("token-1", MESSAGE)).isEqualTo(SendResult.DELIVERED);
        verify(messaging).send(any(Message.class));
    }

    @ParameterizedTest
    @CsvSource({"UNREGISTERED,INVALID_TOKEN", "INVALID_ARGUMENT,INVALID_TOKEN", "UNAVAILABLE,FAILED",
            "QUOTA_EXCEEDED,FAILED"})
    void traduceLosErroresDeFcm(MessagingErrorCode code, SendResult expected) throws Exception {
        FirebaseMessagingException error = mock(FirebaseMessagingException.class);
        when(error.getMessagingErrorCode()).thenReturn(code);
        when(messaging.send(any(Message.class))).thenThrow(error);

        assertThat(new FcmPushSender(messaging).send("token-1", MESSAGE)).isEqualTo(expected);
    }
}
