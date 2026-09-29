package com.pingprint.printer;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "printer_provider_connections")
public class PrinterProviderConnection {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "printer_id", nullable = false, unique = true) private Printer printer;
    @Column(nullable = false, length = 32) private String provider;
    @Column(name = "provider_device_id") private String providerDeviceId;
    @Column(name = "encrypted_access_token", nullable = false, columnDefinition = "TEXT") private String encryptedAccessToken;
    @Column(name = "encrypted_refresh_token", nullable = false, columnDefinition = "TEXT") private String encryptedRefreshToken;
    @Column(name = "access_token_expires_at", nullable = false) private Instant accessTokenExpiresAt;
    @Column(name = "refresh_token_expires_at") private Instant refreshTokenExpiresAt;
    @Column(name = "reauthorization_required", nullable = false) private boolean reauthorizationRequired;
    @Column(nullable = false) private boolean connected;
    @Column(name = "capabilities_json", columnDefinition = "TEXT") private String capabilitiesJson;
    @Column(name = "capabilities_refreshed_at") private Instant capabilitiesRefreshedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected PrinterProviderConnection() { }
    public PrinterProviderConnection(Printer printer, String provider) {
        this.printer = printer; this.provider = provider; this.encryptedAccessToken = ""; this.encryptedRefreshToken = "";
        this.accessTokenExpiresAt = Instant.EPOCH; this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public UUID getId() { return id; }
    public Printer getPrinter() { return printer; }
    public String getEncryptedAccessToken() { return encryptedAccessToken; }
    public String getEncryptedRefreshToken() { return encryptedRefreshToken; }
    public Instant getAccessTokenExpiresAt() { return accessTokenExpiresAt; }
    public Instant getRefreshTokenExpiresAt() { return refreshTokenExpiresAt; }
    public boolean isReauthorizationRequired() { return reauthorizationRequired; }
    public boolean isConnected() { return connected; }
    public String getCapabilitiesJson() { return capabilitiesJson; }
    public Instant getCapabilitiesRefreshedAt() { return capabilitiesRefreshedAt; }
    public String getProviderDeviceId() { return providerDeviceId; }
    public void authorize(String access, String refresh, Instant accessExpiry, Instant refreshExpiry) {
        encryptedAccessToken = access; encryptedRefreshToken = refresh; accessTokenExpiresAt = accessExpiry;
        refreshTokenExpiresAt = refreshExpiry; reauthorizationRequired = false; connected = true; updatedAt = Instant.now();
    }
    public void updateTokens(String access, String refresh, Instant accessExpiry, Instant refreshExpiry) { authorize(access, refresh, accessExpiry, refreshExpiry); }
    public void updateDevice(String deviceId, boolean connected) { providerDeviceId = deviceId; this.connected = connected; updatedAt = Instant.now(); }
    public void updateCapabilities(String json) { capabilitiesJson = json; capabilitiesRefreshedAt = Instant.now(); updatedAt = Instant.now(); }
    public void requireReauthorization() { reauthorizationRequired = true; connected = false; updatedAt = Instant.now(); }
}
