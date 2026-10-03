package com.pingprint.printjob;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/print-orders")
public class PrintOrderController {
    private final PrintOrderService service;
    private final PrintOrderRepository orders;
    private final PrintJobRepository jobs;
    public PrintOrderController(PrintOrderService service, PrintOrderRepository orders, PrintJobRepository jobs) { this.service = service; this.orders = orders; this.jobs = jobs; }

    public record Request(@Min(1) int copies, UUID printerId, UUID documentId, UUID mediaConfigId, String paperSize, String paperType, String colorMode, boolean duplex, String orientation, String pageRange) { }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody Request request, Authentication authentication) {
        UUID userId = authentication != null && authentication.getPrincipal() instanceof UUID id ? id : null;
        PrintOrder order = service.create(userId, new PrintOrderService.CreateRequest(request.printerId(), request.documentId(), request.mediaConfigId(), request.copies(), request.paperSize(), request.paperType(), request.colorMode(), request.duplex(), request.orientation(), request.pageRange()));
        return Map.of("id", order.getId(), "amountMinor", order.getAmountMinor(), "currency", order.getCurrency(), "status", order.getStatus(), "totalPages", order.getTotalPages(), "copies", order.getCopies(), "printerId", order.getPrinterId(), "fileName", order.getFileName());
    }

    @GetMapping("/{id}/status")
    public Map<String, Object> status(@PathVariable UUID id, Authentication authentication) {
        PrintOrder order = orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Print order not found"));
        UUID userId = authentication != null && authentication.getPrincipal() instanceof UUID value ? value : null;
        if (order.getUserId() != null && !order.getUserId().equals(userId)) throw new org.springframework.security.access.AccessDeniedException("Order does not belong to this user");
        PrintJob job = jobs.findByOrderId(id).orElse(null);
        return Map.of(
            "orderId", order.getId(),
            "orderStatus", order.getStatus(),
            "printJobStatus", job == null ? "NOT_CREATED" : job.getStatus(),
            "pagesCompleted", job == null ? 0 : job.getPagesCompleted(),
            "totalPages", order.getTotalPages(),
            "failureReason", job == null || job.getFailureReason() == null ? "" : job.getFailureReason()
        );
    }
}
