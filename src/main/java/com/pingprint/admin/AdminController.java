package com.pingprint.admin;

import com.pingprint.printer.Printer;
import com.pingprint.printer.PrinterRepository;
import com.pingprint.printer.PrinterStatus;
import com.pingprint.printer.PrinterProviderConnectionRepository;
import com.pingprint.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final JwtService jwt;
    private final PrinterRepository printers;
    private final JdbcTemplate db;
    private final PrinterProviderConnectionRepository connections;

    public AdminController(JwtService jwt, PrinterRepository printers, JdbcTemplate db, PrinterProviderConnectionRepository connections) { this.jwt = jwt; this.printers = printers; this.db = db; this.connections = connections; }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) { }
    public record AddPrinterRequest(@NotBlank String name, @NotBlank String location, String provider, String agentKey) { }
    public record StatusRequest(@NotBlank String status) { }
    public record UpdatePrinterRequest(@NotBlank String name, @NotBlank String location, boolean active) { }
    public record PricingRequest(@Min(1) long priceBwMinor, @Min(1) long priceColorMinor, @Min(1) int maxFileMb) { }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequest request) {
        if (!"admin".equals(request.username()) || !"admin".equals(request.password())) throw new IllegalArgumentException("Invalid admin credentials");
        return Map.of("accessToken", jwt.issueAdmin(), "role", "ADMIN");
    }

    @GetMapping("/printers")
    public List<Map<String, Object>> printers() {
        return printers.findAll().stream().map(this::printerView).toList();
    }

    @PostMapping("/printers")
    public Map<String, Object> addPrinter(@Valid @RequestBody AddPrinterRequest request) {
        String provider = request.provider() == null || request.provider().isBlank() ? "EPSON_CONNECT" : request.provider().toUpperCase();
        Printer printer;
        if (provider.equals("LOCAL_AGENT")) {
            if (request.agentKey() == null || request.agentKey().isBlank()) throw new IllegalArgumentException("Agent key is required for local-agent printers");
            printer = new Printer(request.name().trim(), request.location().trim(), hash(request.agentKey()));
        } else if (provider.equals("EPSON_CONNECT")) printer = Printer.cloud(request.name().trim(), request.location().trim(), provider);
        else throw new IllegalArgumentException("Unsupported print provider");
        return printerView(printers.save(printer));
    }

    @PutMapping("/printers/{id}")
    public Map<String, Object> updatePrinter(@PathVariable UUID id, @Valid @RequestBody UpdatePrinterRequest request) {
        Printer printer = printers.findById(id).orElseThrow(() -> new IllegalArgumentException("Printer not found"));
        printer.updateDetails(request.name().trim(), request.location().trim(), request.active());
        return printerView(printers.save(printer));
    }

    @PutMapping("/printers/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
        Printer printer = printers.findById(id).orElseThrow(() -> new IllegalArgumentException("Printer not found"));
        printer.setStatus(PrinterStatus.valueOf(request.status().toUpperCase()));
        printers.save(printer);
        return Map.of("id", printer.getId(), "status", printer.getStatus());
    }

    @GetMapping("/report")
    public Map<String, Object> report() {
        Map<String, Object> totals = db.queryForMap("SELECT COUNT(*) AS orders, COALESCE(SUM(amount_minor) FILTER (WHERE status IN ('CAPTURED', 'REFUNDED')), 0) AS collected_minor, COALESCE(SUM(amount_minor) FILTER (WHERE status = 'REFUNDED'), 0) AS refunded_minor FROM payment_transactions");
        List<Map<String, Object>> transactions = db.queryForList("SELECT id, order_id, provider, provider_payment_id, amount_minor, status, created_at FROM payment_transactions ORDER BY created_at DESC LIMIT 100");
        return Map.of("totals", totals, "transactions", transactions);
    }

    @GetMapping("/pricing")
    public Map<String, Object> pricing() {
        return db.queryForMap("SELECT price_bw_minor, price_color_minor, max_file_mb, updated_at FROM master_pricing WHERE id = 1");
    }

    @PutMapping("/pricing")
    public Map<String, Object> updatePricing(@Valid @RequestBody PricingRequest request) {
        db.update("UPDATE master_pricing SET price_bw_minor = ?, price_color_minor = ?, max_file_mb = ?, updated_at = NOW() WHERE id = 1", request.priceBwMinor(), request.priceColorMinor(), request.maxFileMb());
        return pricing();
    }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
    private Map<String, Object> printerView(Printer printer) {
        var connection = connections.findByPrinterId(printer.getId()).orElse(null);
        Map<String, Object> value = new java.util.LinkedHashMap<>();
        value.put("id", printer.getId()); value.put("name", printer.getName()); value.put("location", printer.getLocation());
        value.put("status", printer.getStatus()); value.put("active", printer.isActive()); value.put("provider", printer.getProvider());
        value.put("manufacturer", printer.getManufacturer()); value.put("model", printer.getModel());
        value.put("connectionState", connection == null ? "NOT_CONNECTED" : connection.isReauthorizationRequired() ? "REAUTHORIZATION_REQUIRED" : connection.isConnected() ? "CONNECTED" : "OFFLINE");
        return value;
    }
}
