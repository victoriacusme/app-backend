package ec.nexo.customer.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Token de push de una instalación de la app. Es un identificador sensible: nunca se registra completo en logs. */
public record DeviceToken(String token, UUID customerId, DevicePlatform platform) {

    private static final int VISIBLE_CHARS = 6;

    public DeviceToken {
        Objects.requireNonNull(token);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(platform);
    }

    public static String mask(String token) {
        return "…" + token.substring(Math.max(0, token.length() - VISIBLE_CHARS));
    }

    @Override
    public String toString() {
        return "DeviceToken[token=" + mask(token) + ", customerId=" + customerId + ", platform=" + platform + "]";
    }
}
