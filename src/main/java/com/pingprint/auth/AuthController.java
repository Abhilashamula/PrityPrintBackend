package com.pingprint.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public Object signup(@Valid @RequestBody AuthDtos.SignupRequest request) {
        auth.signup(request);
        return java.util.Map.of("message", "Account created successfully. You can sign in now.");
    }
    @PostMapping("/verify-email")
    public Object verifyEmail(@Valid @RequestBody AuthDtos.VerifyEmailRequest request) {
        auth.verifyEmail(request.token());
        return java.util.Map.of("message", "Email verified. You can now log in.");
    }
    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) { return auth.login(request); }
    @PostMapping("/google")
    public AuthDtos.AuthResponse google(@Valid @RequestBody AuthDtos.GoogleSignupRequest request) { return auth.googleSignup(request); }
    @GetMapping("/me")
    public AuthDtos.UserResponse me(Authentication authentication) { return auth.profile((java.util.UUID) authentication.getPrincipal()); }
}
