package com.pingprint.printjob;

import com.pingprint.document.Document;
import com.pingprint.document.DocumentRepository;
import com.pingprint.printer.Printer;
import com.pingprint.printer.PrinterRepository;
import com.pingprint.printer.PrinterStatus;
import com.pingprint.printer.PrinterMediaConfig;
import com.pingprint.printer.PrinterMediaConfigRepository;
import com.pingprint.user.User;
import com.pingprint.user.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class PrintOrderService {
    private final PrintOrderRepository orders;
    private final DocumentRepository documents;
    private final PrinterRepository printers;
    private final UserRepository users;
    private final PrinterMediaConfigRepository media;

    public PrintOrderService(PrintOrderRepository orders, DocumentRepository documents, PrinterRepository printers, UserRepository users, JdbcTemplate db, PrinterMediaConfigRepository media) {
        this.orders = orders; this.documents = documents; this.printers = printers; this.users = users; this.media = media;
    }

    public record CreateRequest(UUID printerId, UUID documentId, UUID mediaConfigId, int copies, String paperSize, String paperType, String colorMode, boolean duplex, String orientation, String pageRange) { }

    @Transactional
    public PrintOrder create(UUID userId, CreateRequest request) {
        if (request.copies() < 1 || request.copies() > 100) throw new IllegalArgumentException("Copies must be between 1 and 100");
        String colorMode = value(request.colorMode(), "BW").toUpperCase();
        if (!colorMode.equals("BW") && !colorMode.equals("COLOR")) throw new IllegalArgumentException("Unsupported color mode");
        String orientation = value(request.orientation(), "PORTRAIT").toUpperCase();
        if (!orientation.equals("PORTRAIT") && !orientation.equals("LANDSCAPE")) throw new IllegalArgumentException("Unsupported orientation");
        User user = userId == null ? null : users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Printer printer = printers.findById(request.printerId()).orElseThrow(() -> new IllegalArgumentException("Printer not found"));
        if (!printer.isActive() || (printer.getStatus() != PrinterStatus.ONLINE && printer.getStatus() != PrinterStatus.BUSY)) throw new IllegalStateException("Selected printer is unavailable");
        Document document = documents.findById(request.documentId()).orElseThrow(() -> new IllegalArgumentException("Document not found"));
        if (document.getUser() != null && (userId == null || !document.getUser().getId().equals(userId))) throw new IllegalArgumentException("Document does not belong to this user");
        String pageRange = normalizePageRange(request.pageRange(), document.getPageCount());
        int selectedPages = pageRange == null ? document.getPageCount() : countPages(pageRange);
        PrinterMediaConfig selected = selectMedia(printer.getId(), request);
        if (request.duplex() && !selected.isDuplexSupported()) throw new IllegalArgumentException("Duplex printing is not supported for the selected media");
        if (colorMode.equals("COLOR") && !selected.isColorSupported()) throw new IllegalArgumentException("Color printing is not supported for the selected media");
        if (colorMode.equals("BW") && !selected.isMonoSupported()) throw new IllegalArgumentException("Black and white printing is not supported for the selected media");
        long pricePerPage = colorMode.equals("COLOR") ? selected.getPriceColorMinor() : selected.getPriceBwMinor();
        long amount = Math.multiplyExact(Math.multiplyExact((long) selectedPages, request.copies()), pricePerPage);
        return orders.save(new PrintOrder(user, printer, document, selected, request.copies(), colorMode, request.duplex(), orientation, pageRange, selectedPages, amount));
    }

    private String normalizePageRange(String raw, int maxPages) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("all")) return null;
        java.util.Set<Integer> pages = new java.util.TreeSet<>();
        for (String segment : raw.split(",")) {
            String part = segment.trim();
            if (part.matches("\\d+")) {
                int page = Integer.parseInt(part);
                if (page < 1 || page > maxPages) throw new IllegalArgumentException("Page range is outside the document");
                pages.add(page);
            } else if (part.matches("\\d+\\s*-\\s*\\d+")) {
                String[] bounds = part.split("\\s*-\\s*");
                int start = Integer.parseInt(bounds[0]); int end = Integer.parseInt(bounds[1]);
                if (start > end || start < 1 || end > maxPages) throw new IllegalArgumentException("Page range is outside the document");
                for (int page = start; page <= end; page++) pages.add(page);
            } else throw new IllegalArgumentException("Invalid page range");
        }
        if (pages.isEmpty()) throw new IllegalArgumentException("Page range is empty");
        return pages.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    private int countPages(String normalizedRange) { return normalizedRange.split(",").length; }

    private PrinterMediaConfig selectMedia(UUID printerId, CreateRequest request) {
        if (request.mediaConfigId() != null) return media.findByIdAndPrinterIdAndEnabledTrue(request.mediaConfigId(), printerId).orElseThrow(() -> new IllegalArgumentException("Selected media is unavailable"));
        String size = value(request.paperSize(), "").toUpperCase(); String type = value(request.paperType(), "").toUpperCase();
        return media.findByPrinterIdAndEnabledTrueOrderByPaperSizeAscPaperTypeAsc(printerId).stream()
            .filter(item -> item.getPaperSize().equalsIgnoreCase(size) || item.getPaperSize().equalsIgnoreCase("ps_" + size.toLowerCase()))
            .filter(item -> item.getPaperType().equalsIgnoreCase(type) || item.getPaperType().equalsIgnoreCase("pt_" + type.toLowerCase()) || (type.equals("PLAIN") && item.getPaperType().equalsIgnoreCase("pt_plainpaper")))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Selected paper is not loaded or supported"));
    }

    private String value(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
}
