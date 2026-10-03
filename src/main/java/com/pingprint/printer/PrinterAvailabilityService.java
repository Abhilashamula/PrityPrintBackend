package com.pingprint.printer;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class PrinterAvailabilityService {
    private final PrinterProviderConnectionRepository connections;
    private final PrinterMediaConfigRepository media;

    public PrinterAvailabilityService(PrinterProviderConnectionRepository connections, PrinterMediaConfigRepository media) {
        this.connections = connections;
        this.media = media;
    }

    public List<String> studentAvailabilityIssues(Printer printer) {
        List<String> issues = new ArrayList<>();
        if (printer.isArchived()) issues.add("Printer is archived");
        if (!printer.isActive()) issues.add("Printer is disabled");
        if (!List.of(PrinterStatus.ONLINE, PrinterStatus.BUSY).contains(printer.getStatus())) issues.add("Printer is not online");
        if (!"EPSON_CONNECT".equals(printer.getProvider())) {
            issues.add("No production print provider is configured");
        } else {
            var connection = connections.findByPrinter_Id(printer.getId()).orElse(null);
            if (connection == null || !connection.isConnected()) issues.add("Epson account is not connected");
            else if (connection.isReauthorizationRequired()) issues.add("Epson authorization must be renewed");
        }
        if (media.findByPrinter_IdAndEnabledTrueOrderByPaperSizeAscPaperTypeAsc(printer.getId()).isEmpty()) {
            issues.add("No loaded media is enabled");
        }
        return issues;
    }

    public boolean isStudentAvailable(Printer printer) {
        return studentAvailabilityIssues(printer).isEmpty();
    }
}
