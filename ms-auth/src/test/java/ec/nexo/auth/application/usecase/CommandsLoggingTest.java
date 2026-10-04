package ec.nexo.auth.application.usecase;

import ec.nexo.auth.application.port.out.CustomerProvisioningPort.NewCustomer;
import ec.nexo.auth.application.usecase.LoginUseCase.LoginCommand;
import ec.nexo.auth.application.usecase.RegisterUseCase.RegisterCommand;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Los comandos pueden terminar en un log o en una excepción: su toString no debe exponer secretos ni datos personales. */
class CommandsLoggingTest {

    @Test
    void ningunComandoImprimeContrasenasNiDatosPersonales() {
        String login = new LoginCommand("ana", "Nexo2026*", "pixel").toString();
        String register = new RegisterCommand("pedro", "Nexo2026x", "Pedro Ruiz", "1700000001", "pedro@nexo.ec",
                "+593990000001", LocalDate.of(1985, 3, 3), "pixel").toString();
        String newCustomer = new NewCustomer(UUID.randomUUID(), "Pedro Ruiz", "1700000001", "pedro@nexo.ec",
                "+593990000001", LocalDate.of(1985, 3, 3)).toString();

        assertThat(login).contains("ana").doesNotContain("Nexo2026*");
        assertThat(register).contains("pedro")
                .doesNotContain("Nexo2026x", "1700000001", "pedro@nexo.ec", "+593990000001", "Pedro Ruiz");
        assertThat(newCustomer).doesNotContain("1700000001", "pedro@nexo.ec", "+593990000001", "Pedro Ruiz");
    }
}
