package ec.nexo.accounts.infrastructure.adapter.in.web;

import ec.nexo.accounts.application.usecase.GetAccountMovementsUseCase;
import ec.nexo.accounts.application.usecase.GetMyAccountUseCase;
import ec.nexo.accounts.application.usecase.GetMyAccountsUseCase;
import ec.nexo.accounts.application.usecase.MovementCursor;
import ec.nexo.accounts.application.usecase.MovementPage;
import ec.nexo.accounts.application.usecase.OpenDefaultAccountUseCase;
import ec.nexo.accounts.domain.exception.AccountNotFoundException;
import ec.nexo.accounts.domain.model.Account;
import ec.nexo.accounts.domain.model.AccountStatus;
import ec.nexo.accounts.domain.model.AccountType;
import ec.nexo.accounts.domain.model.Movement;
import ec.nexo.accounts.domain.model.MovementType;
import ec.nexo.accounts.infrastructure.config.SecurityConfig;
import ec.nexo.accounts.infrastructure.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AccountController.class, InternalAccountController.class})
@Import(SecurityConfig.class)
@EnableConfigurationProperties(SecurityProperties.class)
@TestPropertySource(properties = "nexo.security.internal-api-key=test-internal-key")
class AccountControllerTest {

    private static final UUID ANA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GetMyAccountsUseCase getMyAccounts;
    @MockitoBean
    private GetMyAccountUseCase getMyAccount;
    @MockitoBean
    private GetAccountMovementsUseCase getAccountMovements;
    @MockitoBean
    private OpenDefaultAccountUseCase openDefaultAccount;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void listaLasCuentasDelSubDelTokenConNumeroEnmascaradoYMontoComoTexto() throws Exception {
        when(getMyAccounts.execute(ANA)).thenReturn(List.of(account(ANA)));

        mvc.perform(get("/accounts").with(as(ANA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].number").value("****4521"))
                .andExpect(jsonPath("$.items[0].balance").value("1250.50"))
                .andExpect(jsonPath("$.items[0].type").value("SAVINGS"))
                .andExpect(jsonPath("$.items[0].isDefault").value(true));
    }

    @Test
    void sinTokenResponde401ConProblemDetail() throws Exception {
        mvc.perform(get("/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("unauthorized"));

        verifyNoInteractions(getMyAccounts);
    }

    @Test
    void unaCuentaAjenaResponde404() throws Exception {
        UUID foreign = UUID.randomUUID();
        when(getMyAccount.execute(ANA, foreign)).thenThrow(new AccountNotFoundException());

        mvc.perform(get("/accounts/{id}", foreign).with(as(ANA)).header(CorrelationIdFilter.HEADER, "abc-123"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(CorrelationIdFilter.HEADER, "abc-123"))
                .andExpect(jsonPath("$.code").value("account-not-found"))
                .andExpect(jsonPath("$.correlationId").value("abc-123"));
    }

    @Test
    void unIdConFormatoInvalidoResponde400() throws Exception {
        mvc.perform(get("/accounts/no-es-uuid").with(as(ANA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid-parameter"));
    }

    @Test
    void losMovimientosDevuelvenUnCursorOpacoQueSePuedeReenviar() throws Exception {
        UUID accountId = UUID.randomUUID();
        Movement movement = new Movement(UUID.randomUUID(), accountId, MovementType.DEBIT, new BigDecimal("12.5"),
                new BigDecimal("1238.00"), "Supermercado", NOW, null);
        MovementCursor next = MovementCursor.of(movement);
        when(getAccountMovements.execute(ANA, accountId, null, 1)).thenReturn(new MovementPage(List.of(movement), next));
        when(getAccountMovements.execute(eq(ANA), eq(accountId), eq(next), isNull()))
                .thenReturn(new MovementPage(List.of(), null));

        String cursor = MovementCursorCodec.encode(next);
        mvc.perform(get("/accounts/{id}/movements", accountId).param("size", "1").with(as(ANA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].amount").value("12.50"))
                .andExpect(jsonPath("$.items[0].bookedAt").value("2026-10-04T12:00:00Z"))
                .andExpect(jsonPath("$.nextCursor").value(cursor));

        mvc.perform(get("/accounts/{id}/movements", accountId).param("cursor", cursor).with(as(ANA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void unCursorManipuladoResponde400() throws Exception {
        mvc.perform(get("/accounts/{id}/movements", UUID.randomUUID()).param("cursor", "basura").with(as(ANA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid-cursor"));
    }

    @Test
    void internalRechazaPeticionesSinApiKeyAunqueTenganJwt() throws Exception {
        // La cadena de /internal no procesa tokens Bearer: un JWT de cliente no sirve para llamar aquí.
        mvc.perform(post("/internal/accounts").header("Authorization", "Bearer token-de-cliente")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"customerId\":\"" + ANA + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/accounts").header("X-Internal-Api-Key", "otra-clave")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"customerId\":\"" + ANA + "\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(openDefaultAccount);
    }

    @Test
    void internalAbreLaCuentaCon201YEsIdempotenteCon200() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(openDefaultAccount.execute(customerId))
                .thenReturn(new OpenDefaultAccountUseCase.Result(account(customerId), true))
                .thenReturn(new OpenDefaultAccountUseCase.Result(account(customerId), false));

        for (var expected : List.of(status().isCreated(), status().isOk())) {
            mvc.perform(post("/internal/accounts").header("X-Internal-Api-Key", "test-internal-key")
                            .contentType(MediaType.APPLICATION_JSON).content("{\"customerId\":\"" + customerId + "\"}"))
                    .andExpect(expected)
                    .andExpect(jsonPath("$.number").value("****4521"));
        }
    }

    @Test
    void internalValidaElCuerpo() throws Exception {
        mvc.perform(post("/internal/accounts").header("X-Internal-Api-Key", "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        verify(openDefaultAccount, org.mockito.Mockito.never()).execute(any());
    }

    @Test
    void elHealthEsPublico() throws Exception {
        // En @WebMvcTest no está el actuator: basta con que la seguridad no responda 401.
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound());
    }

    private static RequestPostProcessor as(UUID customerId) {
        return jwt().jwt(token -> token.subject(customerId.toString()));
    }

    private static Account account(UUID customerId) {
        return Account.restore(UUID.randomUUID(), customerId, "2200014521", AccountType.SAVINGS, "USD",
                new BigDecimal("1250.5"), AccountStatus.ACTIVE, "Ahorros", true, NOW);
    }
}
