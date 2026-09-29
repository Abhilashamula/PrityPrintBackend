package com.pingprint.printer;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "printer_media_config")
public class PrinterMediaConfig {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "printer_id", nullable = false) private Printer printer;
    @Column(name = "paper_source", nullable = false) private String paperSource;
    @Column(name = "paper_size", nullable = false) private String paperSize;
    @Column(name = "paper_type", nullable = false) private String paperType;
    @Column(name = "print_quality", nullable = false) private String printQuality;
    @Column(nullable = false) private boolean borderless;
    @Column(name = "duplex_supported", nullable = false) private boolean duplexSupported;
    @Column(name = "color_supported", nullable = false) private boolean colorSupported;
    @Column(name = "mono_supported", nullable = false) private boolean monoSupported;
    @Column(nullable = false) private boolean enabled;
    @Column(name = "price_bw_minor") private Long priceBwMinor;
    @Column(name = "price_color_minor") private Long priceColorMinor;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected PrinterMediaConfig() { }
    public PrinterMediaConfig(Printer printer, String source, String size, String type, String quality) {
        this.printer = printer; paperSource = source; paperSize = size; paperType = type; printQuality = quality;
        enabled = true; monoSupported = true; colorSupported = true; createdAt = Instant.now(); updatedAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getPrinterId() { return printer.getId(); }
    public String getPaperSource() { return paperSource; }
    public String getPaperSize() { return paperSize; }
    public String getPaperType() { return paperType; }
    public String getPrintQuality() { return printQuality; }
    public boolean isBorderless() { return borderless; }
    public boolean isDuplexSupported() { return duplexSupported; }
    public boolean isColorSupported() { return colorSupported; }
    public boolean isMonoSupported() { return monoSupported; }
    public boolean isEnabled() { return enabled; }
    public Long getPriceBwMinor() { return priceBwMinor; }
    public Long getPriceColorMinor() { return priceColorMinor; }
    public void configure(boolean enabled, boolean duplex, boolean color, boolean mono, boolean borderless, long bw, long colorPrice, String quality) {
        this.enabled = enabled; duplexSupported = duplex; colorSupported = color; monoSupported = mono; this.borderless = borderless;
        priceBwMinor = bw; priceColorMinor = colorPrice; printQuality = quality; updatedAt = Instant.now();
    }
}
