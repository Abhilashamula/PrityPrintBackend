package com.pingprint.api;

import com.pingprint.printer.Printer;
import com.pingprint.printer.PrinterRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
public class DevelopmentTestingController {
    private final PrinterRepository printers;
    private final boolean enabled;

    public DevelopmentTestingController(PrinterRepository printers,
        @Value("${app.development-printer-bypass-enabled:false}") boolean enabled) {
        this.printers = printers;
        this.enabled = enabled;
    }

    @GetMapping("/printer-bypass")
    public Map<String, Object> printerBypass() {
        if (!enabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Printer printer = printers.findAll().stream()
            .filter(value -> !value.isArchived())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Create an admin printer before using the development bypass"));
        return Map.of("id", printer.getId(), "name", printer.getName() + " (upload test)");
    }
}
