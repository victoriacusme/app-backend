package ec.nexo.customer.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Autentica las llamadas servicio a servicio con {@code X-Internal-Api-Key}. No es un bean para que Spring Boot
 * no lo registre como filtro global: solo se agrega a la cadena de {@code /internal/**}.
 */
class InternalApiKeyFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Internal-Api-Key";

    private final byte[] expectedKey;

    InternalApiKeyFilter(String expectedKey) {
        this.expectedKey = expectedKey == null ? new byte[0] : expectedKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        // Sin clave configurada se rechaza todo (falla cerrado). La comparación es de tiempo constante.
        if (expectedKey.length > 0 && provided != null
                && MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            var authentication = UsernamePasswordAuthenticationToken.authenticated("internal-service", null,
                    AuthorityUtils.createAuthorityList("ROLE_INTERNAL"));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }
}
