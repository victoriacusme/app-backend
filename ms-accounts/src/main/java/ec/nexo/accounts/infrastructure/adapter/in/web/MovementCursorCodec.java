package ec.nexo.accounts.infrastructure.adapter.in.web;

import ec.nexo.accounts.application.usecase.MovementCursor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** Serializa el cursor como texto opaco (Base64 URL) para que la app no dependa de su formato interno. */
final class MovementCursorCodec {

    private static final String SEPARATOR = "|";

    private MovementCursorCodec() {
    }

    static String encode(MovementCursor cursor) {
        if (cursor == null) {
            return null;
        }
        String raw = cursor.bookedAt() + SEPARATOR + cursor.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static MovementCursor decode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            int separator = raw.indexOf(SEPARATOR);
            return new MovementCursor(Instant.parse(raw.substring(0, separator)),
                    UUID.fromString(raw.substring(separator + 1)));
        } catch (RuntimeException e) {
            throw new InvalidCursorException();
        }
    }
}
