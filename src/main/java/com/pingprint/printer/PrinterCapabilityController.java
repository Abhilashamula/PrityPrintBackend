package com.pingprint.printer;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
public class PrinterCapabilityController {
    private final PrinterCapabilityService service;
    public PrinterCapabilityController(PrinterCapabilityService service) { this.service = service; }

    @GetMapping("/api/printers/{printerId}/capabilities")
    public PrinterCapabilityService.Capabilities capabilities(@PathVariable UUID printerId) { return service.studentCapabilities(printerId); }

    @GetMapping("/api/admin/printers/{printerId}/media")
    public List<PrinterCapabilityService.ConfigureMedia> media(@PathVariable UUID printerId) { return service.adminMedia(printerId); }

    @PutMapping("/api/admin/printers/{printerId}/media")
    public PrinterCapabilityService.ConfigureMedia configure(@PathVariable UUID printerId, @Valid @RequestBody PrinterCapabilityService.ConfigureMedia request) { return service.configure(printerId, request); }
}
