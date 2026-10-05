package ec.nexo.accounts.infrastructure.adapter.in.web;

class InvalidIdempotencyKeyException extends RuntimeException {

    InvalidIdempotencyKeyException() {
        super("Falta el header Idempotency-Key o no es válido (1 a 64 caracteres: letras, números, '-' o '_')");
    }
}
