package ec.nexo.customer.infrastructure.adapter.in.web;

import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;

/** ProblemDetail (RFC 7807) con un {@code code} estable para la app y el correlation-id para soporte. */
public final class ProblemDetails {

    private static final String TYPE_BASE = "https://api.nexo.ec/problems/";

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatusCode status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        return enrich(problem, code);
    }

    static ProblemDetail enrich(ProblemDetail problem, String code) {
        problem.setType(URI.create(TYPE_BASE + code));
        problem.setProperty("code", code);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        return problem;
    }
}
