package com.pingprint.admin;

import com.pingprint.printjob.PrintJob;
import com.pingprint.payment.PaymentRefundService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/admin")
public class AdminOperationsController {
    private final AdminOperationsService service;
    private final PaymentRefundService refunds;
    public AdminOperationsController(AdminOperationsService service, PaymentRefundService refunds) { this.service = service; this.refunds = refunds; }
    public record RefundRequest(String reason) { }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() { return service.dashboard(); }

    @GetMapping("/transactions")
    public Map<String, Object> transactions(@RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String search,
        @RequestParam(required = false) String paymentStatus, @RequestParam(required = false) String printStatus,
        @RequestParam(required = false) UUID printerId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "false") boolean includeArchived,
        @RequestParam(defaultValue = "desc") String direction) {
        return service.transactions(page, size, search, paymentStatus, printStatus, printerId, from, to, includeArchived, direction);
    }

    @GetMapping(value = "/transactions/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportTransactions() {
        byte[] csv = service.exportTransactions().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ping-print-transactions.csv")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(csv);
    }

    @GetMapping("/transactions/{id}")
    public Map<String, Object> transaction(@PathVariable UUID id) { return service.transaction(id); }

    @PostMapping("/transactions/{id}/refund")
    public Map<String, Object> refund(@PathVariable UUID id, @RequestBody(required = false) RefundRequest request) {
        Map<String, Object> result = refunds.initiateByAdmin(id, request == null ? null : request.reason());
        service.audit("REFUND_REQUESTED", "PAYMENT_TRANSACTION", id, "Requested refund: " + result.get("refundStatus"));
        return result;
    }

    @PostMapping("/transactions/{id}/archive")
    public Map<String, Object> archiveTransaction(@PathVariable UUID id) {
        service.setTransactionArchived(id, true);
        service.audit("TRANSACTION_ARCHIVED", "PAYMENT_TRANSACTION", id, "Archived transaction from admin views");
        return Map.of("id", id, "archived", true);
    }

    @PostMapping("/transactions/{id}/restore")
    public Map<String, Object> restoreTransaction(@PathVariable UUID id) {
        service.setTransactionArchived(id, false);
        service.audit("TRANSACTION_RESTORED", "PAYMENT_TRANSACTION", id, "Restored transaction to admin views");
        return Map.of("id", id, "archived", false);
    }

    @GetMapping("/printers/{id}/jobs")
    public Map<String, Object> printerJobs(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.printerJobs(id, page, size);
    }

    @PostMapping("/printers/{id}/test-print")
    public Map<String, Object> testPrint(@PathVariable UUID id) {
        PrintJob job = service.submitTest(id);
        return Map.of("jobId", job.getId(), "status", job.getStatus(), "message", "Test print queued for submission.");
    }
}
