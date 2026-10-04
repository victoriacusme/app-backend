package ec.nexo.auth.infrastructure.adapter.in.web;

class InvalidEncryptedPayloadException extends RuntimeException {

    InvalidEncryptedPayloadException(String message) {
        super(message);
    }
}
