package com.pingprint.printjob;

import com.pingprint.document.Document;
import com.pingprint.printer.Printer;
import com.pingprint.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "print_orders")
public class PrintOrder {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "printer_id", nullable = false) private Printer printer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "document_id", nullable = false) private Document document;
    @Column(name = "file_name", nullable = false, length = 255) private String fileName;
    @Column(name = "file_storage_key", nullable = false, length = 512) private String fileStorageKey;
    @Column(name = "total_pages", nullable = false) private int totalPages;
    @Column(nullable = false) private int copies;
    @Column(name = "color_mode", nullable = false, length = 16) private String colorMode;
    @Column(name = "paper_size", nullable = false, length = 16) private String paperSize;
    @Column(name = "paper_type", nullable = false, length = 32) private String paperType;
    @Column(nullable = false) private boolean duplex;
    @Column(nullable = false, length = 16) private String orientation;
    @Column(name = "amount_minor", nullable = false) private long amountMinor;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 24) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "razorpay_order_id", unique = true) private String razorpayOrderId;
    @Column(name = "razorpay_payment_id", unique = true) private String razorpayPaymentId;

    protected PrintOrder() { }

    public PrintOrder(User user, Printer printer, Document document, int copies, String colorMode, String paperSize, String paperType, boolean duplex, String orientation, long amountMinor) {
        this.user = user; this.printer = printer; this.document = document; this.fileName = document.getOriginalFileName(); this.fileStorageKey = document.getStorageKey();
        this.totalPages = document.getPageCount(); this.copies = copies; this.colorMode = colorMode; this.paperSize = paperSize; this.paperType = paperType;
        this.duplex = duplex; this.orientation = orientation; this.amountMinor = amountMinor; this.currency = "INR"; this.status = "CREATED"; this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public long getAmountMinor() { return amountMinor; }
    public int getTotalPages() { return totalPages; }
    public int getCopies() { return copies; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getFileName() { return fileName; }
    public UUID getPrinterId() { return printer.getId(); }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String value) { razorpayOrderId = value; }
    public void setRazorpayPaymentId(String value) { razorpayPaymentId = value; }
    public void markPaid() { status = "PAID"; paidAt = Instant.now(); }
}