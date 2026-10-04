package ec.nexo.accounts.infrastructure.adapter.in.web;

class InvalidCursorException extends RuntimeException {

    InvalidCursorException() {
        super("El cursor de paginación no es válido");
    }
}
