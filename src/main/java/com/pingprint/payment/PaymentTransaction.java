package com.pingprint.payment;

import com.pingprint.printjob.PrintOrder;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_payment_id"}))
public class PaymentTransaction {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private PrintOrder order;
    @Column(nullable = false, length = 32) private String provider;
    @Column(name = "provider_order_id") private String providerOrderId;
    @Column(name = "provider_payment_id") private String providerPaymentId;
    @Column(name = "amount_minor", nullable = false) private long amountMinor;
    @Column(nullable = false, length = 24) private String status;
    @Column(name = "failure_reason") private String failureReason;
    @Column(name = "provider_event_id") private String providerEventId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected PaymentTransaction() { }
    public PaymentTransaction(PrintOrder order, String provider, String providerOrderId, long amountMinor) {
        this.order = order; this.provider = provider; this.providerOrderId = providerOrderId; this.amountMinor = amountMinor; this.status = "CREATED"; this.createdAt = Instant.now();
    }
    public String getStatus() { return status; }
    public void capture(String paymentId) { providerPaymentId = paymentId; status = "CAPTURED"; }
    public void fail(String reason) { status = "FAILED"; failureReason = reason; }
    public void setProviderEventId(String value) { providerEventId = value; }
}