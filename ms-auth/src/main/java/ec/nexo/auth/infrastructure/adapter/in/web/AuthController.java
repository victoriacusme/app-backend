package ec.nexo.auth.infrastructure.adapter.in.web;

import ec.nexo.auth.application.usecase.LoginUseCase;
import ec.nexo.auth.application.usecase.LoginUseCase.LoginCommand;
import ec.nexo.auth.application.usecase.LogoutUseCase;
import ec.nexo.auth.application.usecase.RefreshTokenUseCase;
import ec.nexo.auth.application.usecase.RegisterUseCase;
import ec.nexo.auth.application.usecase.RegisterUseCase.RegisterCommand;
import ec.nexo.auth.infrastructure.adapter.in.web.dto.LoginRequest;
import ec.nexo.auth.infrastructure.adapter.in.web.dto.RefreshTokenRequest;
import ec.nexo.auth.infrastructure.adapter.in.web.dto.RegisterRequest;
import ec.nexo.auth.infrastructure.adapter.in.web.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
class AuthController {

    static final String APPLICATION_JOSE = "application/jose";

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final JweLoginDecrypter jweLoginDecrypter;

    AuthController(LoginUseCase loginUseCase, RegisterUseCase registerUseCase,
                   RefreshTokenUseCase refreshTokenUseCase, LogoutUseCase logoutUseCase,
                   JweLoginDecrypter jweLoginDecrypter) {
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.jweLoginDecrypter = jweLoginDecrypter;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return doLogin(request);
    }

    @PostMapping(path = "/login", consumes = APPLICATION_JOSE)
    TokenResponse loginEncrypted(@RequestBody String compactJwe) {
        return doLogin(jweLoginDecrypter.decrypt(compactJwe));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        var command = new RegisterCommand(request.username(), request.password(), request.fullName(),
                request.idNumber(), request.email(), request.phone(), request.birthDate(), request.deviceId());
        return TokenResponse.from(registerUseCase.execute(command));
    }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return TokenResponse.from(refreshTokenUseCase.execute(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody RefreshTokenRequest request) {
        logoutUseCase.execute(request.refreshToken());
    }

    private TokenResponse doLogin(LoginRequest request) {
        var command = new LoginCommand(request.username(), request.password(), request.deviceId());
        return TokenResponse.from(loginUseCase.execute(command));
    }
}
