package com.example.todoapp.adapter.in.http;

import com.example.todoapp.domain.model.AuthenticatedUser;
import com.example.todoapp.domain.port.in.UserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserUseCase userUseCase;
    private final JwtService jwtService;

    public AuthController(UserUseCase userUseCase, JwtService jwtService) {
        this.userUseCase = userUseCase;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        userUseCase.register(request.username(), request.password());
        AuthenticatedUser authenticated = userUseCase.authenticate(request.username(), request.password());
        return new TokenResponse(jwtService.issue(authenticated));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        AuthenticatedUser authenticated = userUseCase.authenticate(request.username(), request.password());
        return new TokenResponse(jwtService.issue(authenticated));
    }
}
