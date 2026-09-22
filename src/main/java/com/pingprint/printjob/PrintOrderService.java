package com.pingprint.printjob;

import com.pingprint.document.Document;
import com.pingprint.document.DocumentRepository;
import com.pingprint.printer.Printer;
import com.pingprint.printer.PrinterRepository;
import com.pingprint.printer.PrinterStatus;
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
    private final JdbcTemplate db;

    public PrintOrderService(PrintOrderRepository orders, DocumentRepository documents, PrinterRepository printers, UserRepository users, JdbcTemplate db) {
        this.orders = orders; this.documents = documents; this.printers = printers; this.users = users; this.db = db;
    }

    public record CreateRequest(UUID printerId, UUID documentId, int copies, String paperSize, String paperType, String colorMode, boolean duplex, String orientation) { }

    @Transactional
    public PrintOrder create(UUID userId, CreateRequest request) {
        if (request.copies() < 1 || request.copies() > 100) throw new IllegalArgumentException("Copies must be between 1 and 100");
        String colorMode = value(request.colorMode(), "BW").toUpperCase();
        if (!colorMode.equals("BW") && !colorMode.equals("COLOR")) throw new IllegalArgumentException("Unsupported color mode");
        String paperSize = value(request.paperSize(), "A4").toUpperCase();
        if (!paperSize.equals("A4") && !paperSize.equals("A6")) throw new IllegalArgumentException("Unsupported paper size");
        String paperType = value(request.paperType(), "PLAIN").toUpperCase();
        String orientation = value(request.orientation(), "PORTRAIT").toUpperCase();
        if (!orientation.equals("PORTRAIT") && !orientation.equals("LANDSCAPE")) throw new IllegalArgumentException("Unsupported orientation");
        User user = userId == null ? null : users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Printer printer = printers.findById(request.printerId()).orElseThrow(() -> new IllegalArgumentException("Printer not found"));
        if (printer.getStatus() != PrinterStatus.ONLINE && printer.getStatus() != PrinterStatus.BUSY) throw new IllegalStateException("Selected printer is unavailable");
        Document document = documents.findById(request.documentId()).orElseThrow(() -> new IllegalArgumentException("Document not found"));
        if (document.getUser() != null && (userId == null || !document.getUser().getId().equals(userId))) throw new IllegalArgumentException("Document does not belong to this user");
        if (request.duplex() && !printer.isDuplexSupported()) throw new IllegalArgumentException("Duplex printing is not supported by this printer");
        if (colorMode.equals("COLOR") && !printer.isColorSupported()) throw new IllegalArgumentException("Color printing is not supported by this printer");
        long pricePerPage = colorMode.equals("COLOR") ? db.queryForObject("SELECT price_color_minor FROM master_pricing WHERE id = 1", Long.class) : db.queryForObject("SELECT price_bw_minor FROM master_pricing WHERE id = 1", Long.class);
        long amount = Math.multiplyExact(Math.multiplyExact((long) document.getPageCount(), request.copies()), pricePerPage);
        return orders.save(new PrintOrder(user, printer, document, request.copies(), colorMode, paperSize, paperType, request.duplex(), orientation, amount));
    }

    private String value(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
}