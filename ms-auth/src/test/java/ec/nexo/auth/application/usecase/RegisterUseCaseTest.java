package ec.nexo.auth.application.usecase;

import ec.nexo.auth.application.port.out.AccountProvisioningPort;
import ec.nexo.auth.application.port.out.CustomerProvisioningPort;
import ec.nexo.auth.application.port.out.CustomerProvisioningPort.NewCustomer;
import ec.nexo.auth.application.port.out.PasswordHasherPort;
import ec.nexo.auth.application.port.out.ProvisioningFailedException;
import ec.nexo.auth.application.port.out.UserRepositoryPort;
import ec.nexo.auth.application.usecase.RegisterUseCase.RegisterCommand;
import ec.nexo.auth.domain.exception.UsernameTakenException;
import ec.nexo.auth.domain.model.AuthTokens;
import ec.nexo.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-04T15:00:00Z");

    @Mock
    private UserRepositoryPort users;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private CustomerProvisioningPort customerProvisioning;
    @Mock
    private AccountProvisioningPort accountProvisioning;
    @Mock
    private SessionIssuer sessionIssuer;

    private RegisterUseCase registerUseCase;

    @BeforeEach
    void setUp() {
        registerUseCase = new RegisterUseCase(users, passwordHasher, customerProvisioning, accountProvisioning,
                sessionIssuer, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void provisionaClienteYCuentaAntesDeCrearLasCredencialesConElMismoCustomerId() {
        AuthTokens tokens = new AuthTokens("access", Duration.ofMinutes(15), "refresh", Duration.ofDays(7));
        when(users.existsByUsername("pedro")).thenReturn(false);
        when(passwordHasher.hash("Nexo2026x")).thenReturn("argon2-hash");
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionIssuer.open(any(), any())).thenReturn(tokens);

        AuthTokens result = registerUseCase.execute(command(" Pedro "));

        assertThat(result).isSameAs(tokens);
        var newCustomer = ArgumentCaptor.forClass(NewCustomer.class);
        var savedUser = ArgumentCaptor.forClass(User.class);
        InOrder order = inOrder(customerProvisioning, accountProvisioning, users, sessionIssuer);
        order.verify(customerProvisioning).provision(newCustomer.capture());
        order.verify(accountProvisioning).openDefaultAccount(newCustomer.getValue().customerId());
        order.verify(users).save(savedUser.capture());
        order.verify(sessionIssuer).open(savedUser.getValue(), "pixel-9");

        assertThat(newCustomer.getValue().fullName()).isEqualTo("Pedro Ruiz");
        assertThat(savedUser.getValue().username()).isEqualTo("pedro");
        assertThat(savedUser.getValue().customerId()).isEqualTo(newCustomer.getValue().customerId());
        assertThat(savedUser.getValue().passwordHash()).isEqualTo("argon2-hash");
    }

    @Test
    void unUsernameOcupadoNoLlegaAProvisionarNada() {
        when(users.existsByUsername("pedro")).thenReturn(true);

        assertThatThrownBy(() -> registerUseCase.execute(command("pedro")))
                .isInstanceOf(UsernameTakenException.class);

        verifyNoInteractions(customerProvisioning, accountProvisioning, sessionIssuer);
        verify(users, never()).save(any());
    }

    @Test
    void siMsCustomerFallaNoSeAbreCuentaNiSeCreanCredenciales() {
        when(users.existsByUsername("pedro")).thenReturn(false);
        doThrow(new ProvisioningFailedException("ms-customer", new RuntimeException("timeout")))
                .when(customerProvisioning).provision(any());

        assertThatThrownBy(() -> registerUseCase.execute(command("pedro")))
                .isInstanceOf(ProvisioningFailedException.class);

        verifyNoInteractions(accountProvisioning, sessionIssuer);
        verify(customerProvisioning, never()).discard(any());
        verify(users, never()).save(any());
    }

    @Test
    void siMsAccountsFallaNoQuedanCredencialesHuerfanas() {
        when(users.existsByUsername("pedro")).thenReturn(false);
        doThrow(new ProvisioningFailedException("ms-accounts", new RuntimeException("503")))
                .when(accountProvisioning).openDefaultAccount(any());

        assertThatThrownBy(() -> registerUseCase.execute(command("pedro")))
                .isInstanceOf(ProvisioningFailedException.class);

        var newCustomer = ArgumentCaptor.forClass(NewCustomer.class);
        verify(customerProvisioning).provision(newCustomer.capture());
        // Compensación: se descarta el cliente recién creado para no dejarlo huérfano.
        verify(customerProvisioning).discard(newCustomer.getValue().customerId());
        verify(users, never()).save(any());
        verifyNoInteractions(sessionIssuer);
    }

    @Test
    void siLaCompensacionTambienFallaSePropagaElErrorOriginalConLaCausaAdjunta() {
        when(users.existsByUsername("pedro")).thenReturn(false);
        var original = new ProvisioningFailedException("ms-accounts", new RuntimeException("503"));
        var compensation = new ProvisioningFailedException("ms-customer", new RuntimeException("timeout"));
        doThrow(original).when(accountProvisioning).openDefaultAccount(any());
        doThrow(compensation).when(customerProvisioning).discard(any());

        assertThatThrownBy(() -> registerUseCase.execute(command("pedro")))
                .isSameAs(original)
                .satisfies(e -> assertThat(e.getSuppressed()).containsExactly(compensation));
        verify(users, never()).save(any());
    }

    @Test
    void cadaIntentoDeRegistroUsaUnCustomerIdNuevo() {
        when(users.existsByUsername("pedro")).thenReturn(false);
        doThrow(new ProvisioningFailedException("ms-accounts", null)).when(accountProvisioning).openDefaultAccount(any());
        var ids = ArgumentCaptor.forClass(UUID.class);

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> registerUseCase.execute(command("pedro")))
                    .isInstanceOf(ProvisioningFailedException.class);
        }

        verify(accountProvisioning, times(2)).openDefaultAccount(ids.capture());
        assertThat(ids.getAllValues().get(0)).isNotEqualTo(ids.getAllValues().get(1));
    }

    private static RegisterCommand command(String username) {
        return new RegisterCommand(username, "Nexo2026x", "Pedro Ruiz", "1700000001", "pedro@nexo.ec",
                "+593990000001", LocalDate.of(1985, 3, 3), "pixel-9");
    }
}
