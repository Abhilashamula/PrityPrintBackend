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
    @Column(name = "agent_key_hash", unique = true) private String agentKeyHash;
    @Column(length = 80) private String manufacturer;
    @Column(length = 120) private String model;
    @Column(nullable = false, length = 32) private String provider;
    @Column(nullable = false) private boolean active;
    @Column(name = "color_supported", nullable = false) private boolean colorSupported;
    @Column(name = "duplex_supported", nullable = false) private boolean duplexSupported;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private PrinterStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Printer() { }
    public Printer(String name, String location, String agentKeyHash) {
        this.name = name; this.location = location; this.agentKeyHash = agentKeyHash;
        this.colorSupported = true; this.duplexSupported = false; this.provider = "LOCAL_AGENT"; this.active = true;
        this.status = PrinterStatus.OFFLINE; this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public static Printer cloud(String name, String location, String provider) {
        Printer printer = new Printer(name, location, null);
        printer.provider = provider;
        return printer;
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getManufacturer() { return manufacturer; }
    public String getModel() { return model; }
    public String getProvider() { return provider; }
    public boolean isActive() { return active; }
    public PrinterStatus getStatus() { return status; }
    public boolean isColorSupported() { return colorSupported; }
    public boolean isDuplexSupported() { return duplexSupported; }
    public void setStatus(PrinterStatus status) { this.status = status; this.updatedAt = Instant.now(); }
    public void updateDeviceInfo(String manufacturer, String model, boolean connected) {
        this.manufacturer = manufacturer; this.model = model;
        this.status = connected ? PrinterStatus.ONLINE : PrinterStatus.OFFLINE;
        this.updatedAt = Instant.now();
    }
    public void updateDetails(String name, String location, boolean active) {
        this.name = name; this.location = location; this.active = active; this.updatedAt = Instant.now();
    }
}
