package ec.nexo.customer.infrastructure.adapter.in.web;

import ec.nexo.customer.application.usecase.CustomerProfile;
import ec.nexo.customer.application.usecase.GetHomeExperienceUseCase;
import ec.nexo.customer.application.usecase.GetMyProfileUseCase;
import ec.nexo.customer.application.usecase.ProvisionCustomerUseCase;
import ec.nexo.customer.application.usecase.UpdatePreferencesUseCase;
import ec.nexo.customer.domain.exception.CustomerNotFoundException;
import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Experience;
import ec.nexo.customer.domain.model.Preferences;
import ec.nexo.customer.domain.model.Segment;
import ec.nexo.customer.domain.model.Theme;
import ec.nexo.customer.infrastructure.config.SecurityConfig;
import ec.nexo.customer.infrastructure.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CustomerController.class, ExperienceController.class, InternalCustomerController.class})
@Import({SecurityConfig.class, CustomerApiTest.EtagConfig.class})
@EnableConfigurationProperties(SecurityProperties.class)
@TestPropertySource(properties = "nexo.security.internal-api-key=test-internal-key")
class CustomerApiTest {

    private static final UUID ANA = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @TestConfiguration
    static class EtagConfig {
        @Bean
        FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
            var registration = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
            registration.addUrlPatterns("/experience/*");
            return registration;
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GetMyProfileUseCase getMyProfile;
    @MockitoBean
    private UpdatePreferencesUseCase updatePreferences;
    @MockitoBean
    private GetHomeExperienceUseCase getHomeExperience;
    @MockitoBean
    private ProvisionCustomerUseCase provisionCustomer;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void elPerfilSaleConCedulaYTelefonoEnmascarados() throws Exception {
        when(getMyProfile.execute(ANA)).thenReturn(new CustomerProfile(ana(),
                Preferences.restore(ANA, "es", Theme.DARK, true, true)));

        mvc.perform(get("/customers/me").with(as(ANA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.idNumber").value("******5678"))
                .andExpect(jsonPath("$.phone").value("*********4567"))
                .andExpect(jsonPath("$.segment").value("YOUNG"))
                .andExpect(jsonPath("$.preferences.theme").value("DARK"));
    }

    @Test
    void unClienteSinPerfilResponde404() throws Exception {
        when(getMyProfile.execute(ANA)).thenThrow(new CustomerNotFoundException());

        mvc.perform(get("/customers/me").with(as(ANA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("customer-not-found"));
    }

    @Test
    void actualizaPreferenciasParcialmente() throws Exception {
        when(updatePreferences.execute(eq(ANA), eq(new Preferences.Change(null, Theme.LIGHT, null, false))))
                .thenReturn(Preferences.restore(ANA, "es", Theme.LIGHT, true, false));

        mvc.perform(patch("/customers/me/preferences").with(as(ANA)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\":\"LIGHT\",\"showPromotions\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("LIGHT"))
                .andExpect(jsonPath("$.language").value("es"))
                .andExpect(jsonPath("$.showPromotions").value(false));
    }

    @Test
    void validaLasPreferencias() throws Exception {
        mvc.perform(patch("/customers/me/preferences").with(as(ANA)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"fr\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
        mvc.perform(patch("/customers/me/preferences").with(as(ANA)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\":\"ROSADO\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(updatePreferences);
    }

    @Test
    void laExperienciaLlevaETagYResponde304SiNoCambio() throws Exception {
        when(getHomeExperience.execute(ANA)).thenReturn(new Experience("home", Segment.YOUNG, List.of(
                new Experience.Component(UUID.randomUUID(), "greeting", Map.of("title", "Buenas tardes, Ana")),
                new Experience.Component(UUID.randomUUID(), "savings_goal", Map.of("target", "1000.00")))));

        String etag = mvc.perform(get("/experience/home").with(as(ANA)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache, private"))
                .andExpect(jsonPath("$.segment").value("YOUNG"))
                .andExpect(jsonPath("$.components[0].type").value("greeting"))
                .andExpect(jsonPath("$.components[0].props.title").value("Buenas tardes, Ana"))
                .andExpect(jsonPath("$.components[1].type").value("savings_goal"))
                .andReturn().getResponse().getHeader("ETag");

        mvc.perform(get("/experience/home").with(as(ANA)).header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""));
    }

    @Test
    void sinTokenNoHayExperiencia() throws Exception {
        mvc.perform(get("/experience/home"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void elAltaInternaExigeApiKeyYEsIdempotente() throws Exception {
        String body = """
                {"customerId":"%s","fullName":"Pedro Ruiz","idNumber":"1700000001","email":"pedro@nexo.ec",
                 "phone":"+593990000001","birthDate":"1990-01-01"}""".formatted(ANA);

        mvc.perform(post("/internal/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        when(provisionCustomer.execute(any()))
                .thenReturn(new ProvisionCustomerUseCase.Result(ana(), true))
                .thenReturn(new ProvisionCustomerUseCase.Result(ana(), false));
        mvc.perform(post("/internal/customers").header("X-Internal-Api-Key", "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.segment").value("YOUNG"));
        mvc.perform(post("/internal/customers").header("X-Internal-Api-Key", "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void elAltaInternaValidaLosDatos() throws Exception {
        mvc.perform(post("/internal/customers").header("X-Internal-Api-Key", "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + ANA + "\",\"fullName\":\"X\",\"idNumber\":\"123\","
                                + "\"email\":\"no-es-email\",\"phone\":\"1\",\"birthDate\":\"2999-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.idNumber").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.birthDate").exists());
    }

    private static RequestPostProcessor as(UUID customerId) {
        return jwt().jwt(token -> token.subject(customerId.toString()));
    }

    private static Customer ana() {
        return new Customer(ANA, "Ana Torres", "1712345678", "ana@nexo.ec", "+593991234567",
                LocalDate.of(2003, 5, 14), Segment.YOUNG, Instant.now());
    }
}
