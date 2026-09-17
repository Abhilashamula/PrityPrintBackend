package com.pingprint.printer;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/printers")
public class PrinterController {
    private final PrinterRepository printers;
    public PrinterController(PrinterRepository printers) { this.printers = printers; }

    @GetMapping
    public List<Map<String, Object>> available() {
        return printers.findByStatusInOrderByNameAsc(List.of(PrinterStatus.ONLINE, PrinterStatus.BUSY)).stream()
            .map(printer -> Map.<String, Object>of("id", printer.getId(), "name", printer.getName(), "location", printer.getLocation(), "status", printer.getStatus()))
            .toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        Printer printer = printers.findById(id).orElseThrow(() -> new IllegalArgumentException("Printer not found"));
        return Map.of("id", printer.getId(), "name", printer.getName(), "location", printer.getLocation(), "status", printer.getStatus());
    }
}