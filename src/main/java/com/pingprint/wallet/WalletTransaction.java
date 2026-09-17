package com.pingprint.wallet;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;
    @Column(nullable = false, length = 32)
    private String type;
    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;
    @Column(name = "balance_after_minor", nullable = false)
    private long balanceAfterMinor;
    private String provider;
    @Column(name = "provider_reference")
    private String providerReference;
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    protected WalletTransaction() { }
    public WalletTransaction(Wallet wallet, String type, long amountMinor, long balanceAfterMinor, String provider, String providerReference, String idempotencyKey) {
        this.wallet = wallet; this.type = type; this.amountMinor = amountMinor; this.balanceAfterMinor = balanceAfterMinor; this.provider = provider; this.providerReference = providerReference; this.idempotencyKey = idempotencyKey; this.createdAt = Instant.now();
    }
    public String getType() { return type; }
    public long getAmountMinor() { return amountMinor; }
    public long getBalanceAfterMinor() { return balanceAfterMinor; }
    public String getProvider() { return provider; }
    public Instant getCreatedAt() { return createdAt; }
}
