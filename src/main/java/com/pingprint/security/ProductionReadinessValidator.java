package com.pingprint.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
@Profile("prod")
public class ProductionReadinessValidator {
    @Value("${app.admin.username:}") private String adminUsername;
    @Value("${app.admin.password:}") private String adminPassword;
    @Value("${app.jwt.secret:}") private String jwtSecret;
    @Value("${app.razorpay.key-id:}") private String razorpayKeyId;
    @Value("${app.razorpay.key-secret:}") private String razorpayKeySecret;
    @Value("${app.razorpay.webhook-secret:}") private String razorpayWebhookSecret;
    @Value("${app.epson.api-key:}") private String epsonApiKey;
    @Value("${app.epson.client-id:}") private String epsonClientId;
    @Value("${app.epson.client-secret:}") private String epsonClientSecret;
    @Value("${app.epson.redirect-uri:}") private String epsonRedirectUri;
    @Value("${app.token-encryption-key:}") private String tokenEncryptionKey;
    @Value("${app.frontend-url:}") private String frontendUrl;
    @Value("${app.require-email-verification:false}") private boolean requireEmailVerification;
    @Value("${app.development-printer-bypass-enabled:false}") private boolean developmentPrinterBypassEnabled;

    @PostConstruct
    void validate() {
        List<String> failures = new ArrayList<>();
        requireStrong("ADMIN_USERNAME", adminUsername, 5, List.of("admin"), failures);
        requireStrong("ADMIN_PASSWORD", adminPassword, 16, List.of("admin", "password"), failures);
        requireStrong("JWT_SECRET", jwtSecret, 32, List.of("change-this-development-secret-to-a-long-random-value"), failures);
        requirePresent("RAZORPAY_KEY_ID", razorpayKeyId, failures);
        requirePresent("RAZORPAY_KEY_SECRET", razorpayKeySecret, failures);
        requirePresent("RAZORPAY_WEBHOOK_SECRET", razorpayWebhookSecret, failures);
        requirePresent("EPSON_API_KEY", epsonApiKey, failures);
        requirePresent("EPSON_CLIENT_ID", epsonClientId, failures);
        requirePresent("EPSON_CLIENT_SECRET", epsonClientSecret, failures);
        if (!isHttps(epsonRedirectUri)) failures.add("EPSON_REDIRECT_URI must use HTTPS");
        if (!allHttps(frontendUrl)) failures.add("FRONTEND_URL must contain only HTTPS origins");
        if (!isEncryptionKeyValid(tokenEncryptionKey)) failures.add("TOKEN_ENCRYPTION_KEY must be a Base64-encoded 32-byte key");
        if (!requireEmailVerification) failures.add("REQUIRE_EMAIL_VERIFICATION must be true");
        if (developmentPrinterBypassEnabled) failures.add("DEVELOPMENT_PRINTER_BYPASS_ENABLED must be false");
        if (!failures.isEmpty()) throw new IllegalStateException("Production configuration is incomplete: " + String.join("; ", failures));
    }

    private void requirePresent(String name, String value, List<String> failures) {
        if (value == null || value.isBlank() || value.startsWith("replace") || value.startsWith("your-")) failures.add(name + " is required");
    }
    private void requireStrong(String name, String value, int length, List<String> forbidden, List<String> failures) {
        if (value == null || value.length() < length || forbidden.stream().anyMatch(value::equalsIgnoreCase)) failures.add(name + " is not production-safe");
    }
    private boolean isHttps(String value) { return value != null && value.startsWith("https://"); }
    private boolean allHttps(String value) { return value != null && !value.isBlank() && java.util.Arrays.stream(value.split(",")).map(String::trim).allMatch(this::isHttps); }
    private boolean isEncryptionKeyValid(String value) {
        try { return Base64.getDecoder().decode(value).length == 32; }
        catch (RuntimeException ignored) { return false; }
    }
}
