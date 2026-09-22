package com.pingprint.printer;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "printers")
public class Printer {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false) private String location;
    @Column(name = "agent_key_hash", nullable = false, unique = true) private String agentKeyHash;
    @Column(name = "color_supported", nullable = false) private boolean colorSupported;
    @Column(name = "duplex_supported", nullable = false) private boolean duplexSupported;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private PrinterStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Printer() { }
    public Printer(String name, String location, String agentKeyHash) {
        this.name = name; this.location = location; this.agentKeyHash = agentKeyHash;
        this.colorSupported = true; this.duplexSupported = false;
        this.status = PrinterStatus.OFFLINE; this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public PrinterStatus getStatus() { return status; }
    public boolean isColorSupported() { return colorSupported; }
    public boolean isDuplexSupported() { return duplexSupported; }
    public void setStatus(PrinterStatus status) { this.status = status; this.updatedAt = Instant.now(); }
}