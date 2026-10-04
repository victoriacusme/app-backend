package ec.nexo.auth.infrastructure.adapter.in.web;

import ec.nexo.auth.infrastructure.adapter.out.security.RsaKeys;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/** Publica la clave de firma ({@code sig}) para validar JWT y la de cifrado ({@code enc}) para el login JWE. */
@RestController
class JwksController {

    private final RsaKeys keys;

    JwksController(RsaKeys keys) {
        this.keys = keys;
    }

    @GetMapping("/.well-known/jwks.json")
    ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(keys.publicJwkSet().toJSONObject());
    }
}
