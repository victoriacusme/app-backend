package ec.nexo.accounts.infrastructure.adapter.in.web;

import ec.nexo.accounts.application.usecase.GetTransferUseCase;
import ec.nexo.accounts.application.usecase.TransferBetweenOwnAccountsUseCase;
import ec.nexo.accounts.application.usecase.TransferBetweenOwnAccountsUseCase.Result;
import ec.nexo.accounts.application.usecase.TransferBetweenOwnAccountsUseCase.TransferCommand;
import ec.nexo.accounts.domain.exception.AccountNotOwnedException;
import ec.nexo.accounts.domain.exception.IdempotencyKeyReusedException;
import ec.nexo.accounts.domain.exception.InsufficientFundsException;
import ec.nexo.accounts.domain.exception.TransferNotFoundException;
import ec.nexo.accounts.domain.model.Transfer;
import ec.nexo.accounts.domain.model.TransferStatus;
import ec.nexo.accounts.infrastructure.config.SecurityConfig;
import ec.nexo.accounts.infrastructure.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransferController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(SecurityProperties.class)
class TransferControllerTest {

    private static final UUID ANA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SOURCE = UUID.randomUUID();
    private static final UUID TARGET = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TransferBetweenOwnAccountsUseCase transferBetweenOwnAccounts;
    @MockitoBean
    private GetTransferUseCase getTransfer;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void creaLaTransferenciaCon201YLocation() throws Exception {
        Transfer transfer = transfer();
        when(transferBetweenOwnAccounts.execute(any())).thenReturn(new Result(transfer, false));

        mvc.perform(transferRequest("key-1", "25.5", "  Ahorro  "))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/transfers/" + transfer.id()))
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value("25.50"));

        var command = ArgumentCaptor.forClass(TransferCommand.class);
        verify(transferBetweenOwnAccounts).execute(command.capture());
        assertThat(command.getValue().customerId()).isEqualTo(ANA);
        assertThat(command.getValue().idempotencyKey()).isEqualTo("key-1");
        assertThat(command.getValue().amount()).isEqualByComparingTo("25.50");
        assertThat(command.getValue().description()).isEqualTo("Ahorro");
    }

    @Test
    void unReintentoResponde200ConIdempotentReplayed() throws Exception {
        when(transferBetweenOwnAccounts.execute(any())).thenReturn(new Result(transfer(), true));

        mvc.perform(transferRequest("key-1", "25.50", null))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"));
    }

    @Test
    void exigeUnaIdempotencyKeyValida() throws Exception {
        for (String key : new String[]{null, "", "con espacios", "x".repeat(65)}) {
            mvc.perform(transferRequest(key, "10.00", null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("invalid-idempotency-key"));
        }
        verifyNoInteractions(transferBetweenOwnAccounts);
    }

    @Test
    void rechazaMontosMalFormados() throws Exception {
        for (String amount : new String[]{"10,50", "1e3", "-5", "10.555", "abc"}) {
            mvc.perform(transferRequest("key-2", amount, null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("validation-error"))
                    .andExpect(jsonPath("$.errors.amount").exists());
        }
        verifyNoInteractions(transferBetweenOwnAccounts);
    }

    @Test
    void traduceLosErroresDeNegocioAlCodigoHttpEsperado() throws Exception {
        expectError(new AccountNotOwnedException(), status().isForbidden(), "account-not-owned");
        expectError(new InsufficientFundsException(), status().isUnprocessableEntity(), "insufficient-funds");
        expectError(new IdempotencyKeyReusedException(), status().isConflict(), "idempotency-key-reused");
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(post("/transfers/own").header("Idempotency-Key", "k")
                        .contentType(MediaType.APPLICATION_JSON).content(body("10.00", null)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(transferBetweenOwnAccounts);
    }

    @Test
    void consultaElEstadoDeUnaTransferenciaPropia() throws Exception {
        Transfer transfer = transfer();
        when(getTransfer.execute(ANA, transfer.id())).thenReturn(transfer);

        mvc.perform(get("/transfers/{id}", transfer.id()).with(jwt().jwt(t -> t.subject(ANA.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transfer.id().toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void unaTransferenciaAjenaResponde404() throws Exception {
        UUID transferId = UUID.randomUUID();
        when(getTransfer.execute(ANA, transferId)).thenThrow(new TransferNotFoundException());

        mvc.perform(get("/transfers/{id}", transferId).with(jwt().jwt(t -> t.subject(ANA.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("transfer-not-found"));
    }

    private void expectError(RuntimeException error, ResultMatcher status, String code) throws Exception {
        doThrow(error).when(transferBetweenOwnAccounts).execute(any());
        mvc.perform(transferRequest("key-3", "10.00", null))
                .andExpect(status)
                .andExpect(jsonPath("$.code").value(code));
    }

    private static MockHttpServletRequestBuilder transferRequest(String key, String amount, String description) {
        var request = post("/transfers/own")
                .with(jwt().jwt(t -> t.subject(ANA.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(amount, description));
        return key == null ? request : request.header("Idempotency-Key", key);
    }

    private static String body(String amount, String description) {
        return """
                {"sourceAccountId":"%s","targetAccountId":"%s","amount":"%s"%s}"""
                .formatted(SOURCE, TARGET, amount, description == null ? "" : ",\"description\":\"" + description + "\"");
    }

    private static Transfer transfer() {
        return new Transfer(UUID.randomUUID(), ANA, "key-1", "hash", SOURCE, TARGET, new BigDecimal("25.50"), "USD",
                "Ahorro", TransferStatus.COMPLETED, Instant.parse("2026-10-04T12:00:00Z"));
    }
}
