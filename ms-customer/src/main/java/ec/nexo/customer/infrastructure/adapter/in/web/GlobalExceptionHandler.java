package ec.nexo.customer.infrastructure.adapter.in.web;

import ec.nexo.customer.domain.exception.CustomerException;
import ec.nexo.customer.domain.exception.CustomerNotFoundException;
import ec.nexo.customer.domain.exception.InvalidNotificationException;
import ec.nexo.customer.domain.exception.InvalidPreferenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce los errores a ProblemDetail con un {@code code} estable para la app. */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomerException.class)
    ProblemDetail handleCustomer(CustomerException e) {
        return switch (e) {
            case CustomerNotFoundException ex -> ProblemDetails.of(HttpStatus.NOT_FOUND, "customer-not-found", ex.getMessage());
            case InvalidPreferenceException ex -> ProblemDetails.of(HttpStatus.BAD_REQUEST, "invalid-preference", ex.getMessage());
            case InvalidNotificationException ex -> ProblemDetails.of(HttpStatus.BAD_REQUEST, "invalid-notification", ex.getMessage());
            default -> ProblemDetails.of(HttpStatus.BAD_REQUEST, "customer-error", e.getMessage());
        };
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "invalid-parameter",
                "El parámetro '" + e.getName() + "' no tiene un formato válido");
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(ProblemDetails.of(HttpStatus.UNAUTHORIZED, "unauthorized", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        log.error("Error no controlado", e);
        return ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Ocurrió un error inesperado. Intenta nuevamente.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException e,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, "validation-error",
                "La solicitud tiene datos inválidos");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /** Los errores estándar de Spring MVC (405, 415, cuerpo ilegible...) también llevan {@code code}. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(@NonNull Exception e, @Nullable Object body,
                                                             @NonNull HttpHeaders headers,
                                                             @NonNull HttpStatusCode status,
                                                             @NonNull WebRequest request) {
        if (body instanceof ProblemDetail problem && (problem.getProperties() == null
                || !problem.getProperties().containsKey("code"))) {
            String code = HttpStatus.resolve(status.value()) == null
                    ? "http-" + status.value()
                    : HttpStatus.valueOf(status.value()).name().toLowerCase().replace('_', '-');
            ProblemDetails.enrich(problem, code);
        }
        return super.handleExceptionInternal(e, body, headers, status, request);
    }
}
