package com.pingprint.admin;

import com.pingprint.printer.Printer;
import com.pingprint.printer.PrinterRepository;
import com.pingprint.printer.PrinterStatus;
import com.pingprint.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

    public AdminController(JwtService jwt, PrinterRepository printers, JdbcTemplate db) { this.jwt = jwt; this.printers = printers; this.db = db; }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) { }
    public record AddPrinterRequest(@NotBlank String name, @NotBlank String location, @NotBlank String agentKey) { }
    public record StatusRequest(@NotBlank String status) { }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequest request) {
        if (!"admin".equals(request.username()) || !"admin".equals(request.password())) throw new IllegalArgumentException("Invalid admin credentials");
        return Map.of("accessToken", jwt.issueAdmin(), "role", "ADMIN");
    }

    @GetMapping("/printers")
    public List<Map<String, Object>> printers() {
        return printers.findAll().stream().map(printer -> Map.<String, Object>of("id", printer.getId(), "name", printer.getName(), "location", printer.getLocation(), "status", printer.getStatus())).toList();
    }

    @PostMapping("/printers")
    public Map<String, Object> addPrinter(@Valid @RequestBody AddPrinterRequest request) {
        Printer printer = printers.save(new Printer(request.name().trim(), request.location().trim(), hash(request.agentKey())));
        return Map.of("id", printer.getId(), "name", printer.getName(), "location", printer.getLocation(), "status", printer.getStatus(), "agentKey", request.agentKey());
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

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
}