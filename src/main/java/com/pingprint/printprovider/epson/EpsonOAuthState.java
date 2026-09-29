package com.pingprint.printprovider.epson;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "epson_oauth_states")
public class EpsonOAuthState {
    @Id @Column(name = "state_hash", length = 128) private String stateHash;
    @Column(name = "printer_id", nullable = false) private UUID printerId;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected EpsonOAuthState() { }
    public EpsonOAuthState(String hash, UUID printerId) { stateHash = hash; this.printerId = printerId; expiresAt = Instant.now().plusSeconds(600); createdAt = Instant.now(); }
    public UUID getPrinterId() { return printerId; }
    public boolean isExpired() { return expiresAt.isBefore(Instant.now()); }
}
