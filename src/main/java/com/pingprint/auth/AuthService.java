package com.pingprint.auth;

import com.pingprint.user.User;
import com.pingprint.user.UserRepository;
import com.pingprint.wallet.Wallet;
import com.pingprint.wallet.WalletRepository;
import com.pingprint.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.net.URI;
import org.springframework.web.client.RestClient;

@Service
public class AuthService {
    private final UserRepository users;
    private final WalletRepository wallets;
    private final EmailVerificationTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long verificationHours;
    private final boolean requireEmailVerification;
    private final String googleClientId;

    public AuthService(UserRepository users, WalletRepository wallets, EmailVerificationTokenRepository tokens, PasswordEncoder passwordEncoder, JwtService jwtService, @Value("${app.email-verification-hours}") long verificationHours, @Value("${app.google-client-id}") String googleClientId, @Value("${app.require-email-verification}") boolean requireEmailVerification) {
        this.users = users; this.wallets = wallets; this.tokens = tokens; this.passwordEncoder = passwordEncoder; this.jwtService = jwtService; this.verificationHours = verificationHours; this.googleClientId = googleClientId; this.requireEmailVerification = requireEmailVerification;
    }

    @Transactional
    public AuthDtos.AuthResponse googleSignup(AuthDtos.GoogleSignupRequest request) {
        if (googleClientId == null || googleClientId.isBlank()) throw new IllegalStateException("Google sign-up is not configured");
        var claims = RestClient.create().get().uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + request.credential())).retrieve().body(java.util.Map.class);
        if (claims == null || !googleClientId.equals(claims.get("aud")) || !"true".equals(String.valueOf(claims.get("email_verified")))) throw new IllegalArgumentException("Google identity could not be verified");
        String email = String.valueOf(claims.get("email")).trim().toLowerCase();
        User user = users.findByEmailIgnoreCase(email).orElseGet(() -> {
            User created = users.save(new User(email, passwordEncoder.encode(UUID.randomUUID().toString()), request.name().trim(), request.age(), request.gender().toLowerCase(), request.phone().trim()));
            created.verifyEmail();
            wallets.save(new Wallet(created));
            return created;
        });
        if (!user.isEmailVerified()) user.verifyEmail();
        return new AuthDtos.AuthResponse(jwtService.issue(user), toResponse(user));
    }

    @Transactional
    public String signup(AuthDtos.SignupRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new IllegalArgumentException("An account already exists for this email");
        User user = users.save(new User(email, passwordEncoder.encode(request.password()), request.name().trim(), request.age(), request.gender().toLowerCase(), request.phone().trim()));
        wallets.save(new Wallet(user));
        if (!requireEmailVerification) user.verifyEmail();
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        tokens.save(new EmailVerificationToken(user, hash(rawToken), Instant.now().plus(verificationHours, ChronoUnit.HOURS)));
        // Replace this log with an email provider adapter before production.
        System.out.println("Email verification token for " + email + ": " + rawToken);
        return rawToken;
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        EmailVerificationToken token = tokens.findByTokenHash(hash(rawToken)).orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));
        if (!token.isValid()) throw new IllegalArgumentException("Verification token has expired or was already used");
        token.getUser().verifyEmail();
        token.markUsed();
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim()).orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) throw new IllegalArgumentException("Invalid email or password");
        if (requireEmailVerification && !user.isEmailVerified()) throw new IllegalStateException("Verify your email before logging in");
        return new AuthDtos.AuthResponse(jwtService.issue(user), toResponse(user));
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserResponse profile(UUID userId) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toResponse(user);
    }
    public AuthDtos.UserResponse toResponse(User user) {
        Wallet wallet = wallets.findByUserId(user.getId()).orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        return new AuthDtos.UserResponse(user.getId().toString(), user.getEmail(), user.getDisplayName(), user.getAge(), user.getGender(), user.getPhone(), user.isEmailVerified(), new AuthDtos.WalletResponse(wallet.getBalanceMinor(), wallet.getCurrency()));
    }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
