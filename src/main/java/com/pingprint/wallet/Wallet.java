package com.pingprint.wallet;

import com.pingprint.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    @Column(name = "balance_minor", nullable = false)
    private long balanceMinor;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Wallet() { }
    public Wallet(User user) {
        this.user = user;
        this.currency = "INR";
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }
    public UUID getId() { return id; }
    public long getBalanceMinor() { return balanceMinor; }
    public String getCurrency() { return currency; }
    public void credit(long amount) { balanceMinor += amount; updatedAt = Instant.now(); }
    public void debit(long amount) {
        if (amount <= 0 || amount > balanceMinor) throw new IllegalStateException("Insufficient wallet balance");
        balanceMinor -= amount;
        updatedAt = Instant.now();
    }
}
