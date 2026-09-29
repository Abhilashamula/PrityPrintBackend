package com.pingprint.printjob;

import com.pingprint.document.Document;
import com.pingprint.printer.Printer;
import com.pingprint.printer.PrinterMediaConfig;
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
    @Column(name = "page_range", length = 512) private String pageRange;
    @Column(nullable = false) private int copies;
    @Column(name = "color_mode", nullable = false, length = 16) private String colorMode;
    @Column(name = "paper_size", nullable = false, length = 16) private String paperSize;
    @Column(name = "paper_type", nullable = false, length = 32) private String paperType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "media_config_id") private PrinterMediaConfig mediaConfig;
    @Column(name = "paper_source") private String paperSource;
    @Column(name = "print_quality") private String printQuality;
    @Column(nullable = false) private boolean duplex;
    @Column(nullable = false, length = 16) private String orientation;
    @Column(name = "amount_minor", nullable = false) private long amountMinor;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 24) private String status;
    @Column(name = "payment_status", nullable = false, length = 24) private String paymentStatus;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "razorpay_order_id", unique = true) private String razorpayOrderId;
    @Column(name = "razorpay_payment_id", unique = true) private String razorpayPaymentId;

    protected PrintOrder() { }

    public PrintOrder(User user, Printer printer, Document document, PrinterMediaConfig mediaConfig, int copies, String colorMode, boolean duplex, String orientation, String pageRange, int totalPages, long amountMinor) {
        this.user = user; this.printer = printer; this.document = document; this.fileName = document.getOriginalFileName(); this.fileStorageKey = document.getStorageKey();
        this.mediaConfig = mediaConfig; this.paperSource = mediaConfig.getPaperSource(); this.paperSize = mediaConfig.getPaperSize(); this.paperType = mediaConfig.getPaperType(); this.printQuality = mediaConfig.getPrintQuality();
        this.totalPages = totalPages; this.pageRange = pageRange; this.copies = copies; this.colorMode = colorMode;
        this.duplex = duplex; this.orientation = orientation; this.amountMinor = amountMinor; this.currency = "INR"; this.status = "PAYMENT_PENDING"; this.paymentStatus = "PENDING"; this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public long getAmountMinor() { return amountMinor; }
    public int getTotalPages() { return totalPages; }
    public String getPageRange() { return pageRange; }
    public int getCopies() { return copies; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getFileName() { return fileName; }
    public String getFileStorageKey() { return fileStorageKey; }
    public Document getDocument() { return document; }
    public Printer getPrinter() { return printer; }
    public UUID getUserId() { return user == null ? null : user.getId(); }
    public UUID getPrinterId() { return printer.getId(); }
    public String getColorMode() { return colorMode; }
    public String getPaperSize() { return paperSize; }
    public String getPaperType() { return paperType; }
    public String getPaperSource() { return paperSource; }
    public String getPrintQuality() { return printQuality; }
    public boolean isDuplex() { return duplex; }
    public String getOrientation() { return orientation; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String value) { razorpayOrderId = value; }
    public void setRazorpayPaymentId(String value) { razorpayPaymentId = value; }
    public void markPaid() { if (!"CAPTURED".equals(paymentStatus)) { paymentStatus = "CAPTURED"; paidAt = Instant.now(); } if (status.equals("PAYMENT_PENDING") || status.equals("CREATED")) status = "PAID"; }
    public void markPrinting() { status = "PRINTING"; }
    public void markCompleted() { status = "COMPLETED"; completedAt = Instant.now(); }
    public void markPrintFailed() { status = "PRINT_FAILED"; }
    public void markStatusUnknown() { status = "PRINT_STATUS_UNKNOWN"; }
    public void markRefundPending() { status = "REFUND_PENDING"; }
    public void markRefunded() { status = "REFUNDED"; paymentStatus = "REFUNDED"; }
}
