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
    public PrintOrderController(PrintOrderService service) { this.service = service; }

    public record Request(@Min(1) int copies, UUID printerId, UUID documentId, String paperSize, String paperType, String colorMode, boolean duplex, String orientation) { }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody Request request, Authentication authentication) {
        UUID userId = authentication != null && authentication.getPrincipal() instanceof UUID id ? id : null;
        PrintOrder order = service.create(userId, new PrintOrderService.CreateRequest(request.printerId(), request.documentId(), request.copies(), request.paperSize(), request.paperType(), request.colorMode(), request.duplex(), request.orientation()));
        return Map.of("id", order.getId(), "amountMinor", order.getAmountMinor(), "currency", order.getCurrency(), "status", order.getStatus(), "totalPages", order.getTotalPages(), "copies", order.getCopies(), "printerId", order.getPrinterId(), "fileName", order.getFileName());
    }
}