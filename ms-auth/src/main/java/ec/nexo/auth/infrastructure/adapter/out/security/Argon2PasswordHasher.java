package ec.nexo.auth.infrastructure.adapter.out.security;

import ec.nexo.auth.application.port.out.PasswordHasherPort;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/** Argon2id con los parámetros recomendados por Spring Security 5.8+ (OWASP: m=16 MiB, t=2, p=1). */
@Component
class Argon2PasswordHasher implements PasswordHasherPort {

    private final Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return rawPassword != null && encoder.matches(rawPassword, passwordHash);
    }
}
