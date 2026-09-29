package com.pingprint.printprovider.epson;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@RestController
public class EpsonOAuthController {
    private final EpsonAuthService auth;
    private final String frontendUrl;
    public EpsonOAuthController(EpsonAuthService auth, @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) { this.auth = auth; this.frontendUrl = frontendUrl.split(",")[0].trim(); }

    @GetMapping("/api/admin/printers/{printerId}/epson/connect")
    public Map<String, String> connect(@PathVariable UUID printerId) { return Map.of("authorizationUrl", auth.begin(printerId)); }

    @PostMapping("/api/admin/printers/{printerId}/epson/capabilities/refresh")
    public Map<String, Boolean> refresh(@PathVariable UUID printerId) { auth.refreshCapabilities(printerId); return Map.of("refreshed", true); }

    @GetMapping("/api/epson/oauth/callback")
    public RedirectView callback(@RequestParam String code, @RequestParam String state) {
        try {
            UUID printerId = auth.complete(code, state);
            return new RedirectView(frontendUrl + "/admin?epson=connected&printerId=" + printerId);
        } catch (RuntimeException error) {
            return new RedirectView(frontendUrl + "/admin?epson=error&message=" + URLEncoder.encode(error.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
