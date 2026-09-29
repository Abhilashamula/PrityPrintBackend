package com.pingprint.admin;

import com.pingprint.printjob.PrintJob;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminOperationsController {
    private final AdminOperationsService service;
    public AdminOperationsController(AdminOperationsService service) { this.service = service; }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() { return service.dashboard(); }

    @GetMapping("/transactions")
    public Map<String, Object> transactions(@RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String search,
        @RequestParam(required = false) String paymentStatus, @RequestParam(required = false) String printStatus,
        @RequestParam(required = false) UUID printerId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "desc") String direction) {
        return service.transactions(page, size, search, paymentStatus, printStatus, printerId, from, to, direction);
    }

    @GetMapping("/transactions/{id}")
    public Map<String, Object> transaction(@PathVariable UUID id) { return service.transaction(id); }

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
