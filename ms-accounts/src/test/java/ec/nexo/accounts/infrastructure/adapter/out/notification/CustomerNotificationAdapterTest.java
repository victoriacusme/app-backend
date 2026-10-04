package ec.nexo.accounts.infrastructure.adapter.out.notification;

import ec.nexo.accounts.application.port.out.TransferNotification;
import ec.nexo.accounts.infrastructure.adapter.in.web.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CustomerNotificationAdapterTest {

    private static final UUID ANA = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MockRestServiceServer server;
    private CustomerNotificationAdapter adapter;
    private final TransferNotification notification = new TransferNotification(ANA, UUID.randomUUID(),
            new BigDecimal("125.50"), "USD", "****7834");

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ms-customer:8082");
        server = MockRestServiceServer.bindTo(builder).build();
        // Ejecutor síncrono: el test verifica qué se envía y cuándo, sin esperar a otros hilos.
        adapter = new CustomerNotificationAdapter(builder.build(), Runnable::run);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        MDC.clear();
    }

    @Test
    void enviaElEventoAMsCustomerConElCorrelationId() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "demo-123");
        server.expect(requestTo("http://ms-customer:8082/internal/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(CorrelationIdFilter.HEADER, "demo-123"))
                .andExpect(jsonPath("$.customerId").value(ANA.toString()))
                .andExpect(jsonPath("$.type").value("TRANSFER_COMPLETED"))
                .andExpect(jsonPath("$.data.amount").value("125.50"))
                .andExpect(jsonPath("$.data.targetAccount").value("****7834"))
                .andExpect(jsonPath("$.data.transferId").value(notification.transferId().toString()))
                .andRespond(withSuccess());

        adapter.transferCompleted(notification);

        server.verify();
    }

    @Test
    void soloAvisaCuandoLaTransaccionSeConfirma() {
        TransactionSynchronizationManager.initSynchronization();
        adapter.transferCompleted(notification);
        // Todavía no hay commit: no debe haber salido ninguna petición.
        server.verify();

        server.expect(requestTo("http://ms-customer:8082/internal/notifications")).andRespond(withSuccess());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        server.verify();
    }

    @Test
    void siLaTransaccionSeRevierteNoAvisa() {
        TransactionSynchronizationManager.initSynchronization();
        adapter.transferCompleted(notification);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        server.verify();
    }

    @Test
    void unFalloDeMsCustomerNoSePropaga() {
        server.expect(requestTo("http://ms-customer:8082/internal/notifications"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatCode(() -> adapter.transferCompleted(notification)).doesNotThrowAnyException();
        server.verify();
    }
}
