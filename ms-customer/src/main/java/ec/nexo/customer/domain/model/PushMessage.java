package ec.nexo.customer.domain.model;

import java.util.Map;

/** Lo que ve el usuario (título y cuerpo) y los datos que usa la app al tocar la notificación (p. ej. el deep link). */
public record PushMessage(String title, String body, Map<String, String> data) {
}
