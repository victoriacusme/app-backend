package ec.nexo.auth.application.port.out;

/** Un servicio aguas abajo no pudo completar el onboarding (caído, timeout o respuesta de error). */
public class ProvisioningFailedException extends RuntimeException {

    public ProvisioningFailedException(String service, Throwable cause) {
        super("No se pudo completar el onboarding en " + service, cause);
    }
}
