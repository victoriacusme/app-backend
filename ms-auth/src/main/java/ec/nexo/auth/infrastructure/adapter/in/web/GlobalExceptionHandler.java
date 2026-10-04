package ec.nexo.auth.infrastructure.adapter.in.web;

import ec.nexo.auth.application.port.out.ProvisioningFailedException;
import ec.nexo.auth.domain.exception.AuthException;
import ec.nexo.auth.domain.exception.InvalidCredentialsException;
import ec.nexo.auth.domain.exception.InvalidRefreshTokenException;
import ec.nexo.auth.domain.exception.UserLockedException;
import ec.nexo.auth.domain.exception.UsernameTakenException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce los errores a ProblemDetail (RFC 7807) con un {@code code} estable para la app. */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_BASE = "https://api.nexo.ec/problems/";

    @ExceptionHandler(AuthException.class)
    ProblemDetail handleAuth(AuthException e) {
        return switch (e) {
            case InvalidCredentialsException ex -> problem(HttpStatus.UNAUTHORIZED, "invalid-credentials", ex.getMessage());
            case UserLockedException ex -> problem(HttpStatus.LOCKED, "user-locked", ex.getMessage());
            case UsernameTakenException ex -> problem(HttpStatus.CONFLICT, "username-taken", ex.getMessage());
            case InvalidRefreshTokenException ex -> problem(HttpStatus.UNAUTHORIZED, "invalid-refresh-token", ex.getMessage());
            default -> problem(HttpStatus.BAD_REQUEST, "auth-error", e.getMessage());
        };
    }

    @ExceptionHandler(ProvisioningFailedException.class)
    ProblemDetail handleProvisioning(ProvisioningFailedException e) {
        log.error("Fallo en el onboarding: {}", e.getMessage(), e.getCause());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "onboarding-unavailable",
                "No pudimos completar el registro en este momento. Intenta nuevamente.");
    }

    @ExceptionHandler(InvalidEncryptedPayloadException.class)
    ProblemDetail handleInvalidJwe(InvalidEncryptedPayloadException e) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-encrypted-payload", e.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation-error", "La solicitud tiene datos inválidos");
        problem.setProperty("errors", errors);
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException e,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation-error", "La solicitud tiene datos inválidos");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_BASE + code));
        problem.setProperty("code", code);
        return problem;
    }
}
