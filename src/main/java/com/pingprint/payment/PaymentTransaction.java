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
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "refund_id") private String refundId;
    @Column(name = "refund_amount_minor") private Long refundAmountMinor;
    @Column(name = "refund_reason") private String refundReason;
    @Column(name = "refunded_at") private Instant refundedAt;
    @Column(name = "refund_status", length = 24) private String refundStatus;
    @Column(name = "refund_failure_reason", length = 500) private String refundFailureReason;

    protected PaymentTransaction() { }
    public PaymentTransaction(PrintOrder order, String provider, String providerOrderId, long amountMinor) {
        this.order = order; this.provider = provider; this.providerOrderId = providerOrderId; this.amountMinor = amountMinor; this.status = "CREATED"; this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public String getStatus() { return status; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public String getProviderOrderId() { return providerOrderId; }
    public UUID getOrderId() { return order.getId(); }
    public long getAmountMinor() { return amountMinor; }
    public String getRefundId() { return refundId; }
    public String getRefundStatus() { return refundStatus; }
    public void linkPayment(String paymentId) { providerPaymentId = paymentId; updatedAt = Instant.now(); }
    public void capture(String paymentId) { providerPaymentId = paymentId; status = "CAPTURED"; updatedAt = Instant.now(); }
    public void fail(String reason) { if (!status.equals("CAPTURED") && !status.equals("REFUNDED")) { status = "FAILED"; failureReason = reason; updatedAt = Instant.now(); } }
    public void refundRequested(String id, long amount, String reason) { if (!"PROCESSED".equals(refundStatus)) { refundId = id; refundAmountMinor = amount; refundReason = reason; refundStatus = "PENDING"; refundFailureReason = null; updatedAt = Instant.now(); } }
    public void refundProcessed(String id, long amount) { refundId = id; refundAmountMinor = amount; refundStatus = "PROCESSED"; refundFailureReason = null; status = "REFUNDED"; refundedAt = Instant.now(); updatedAt = refundedAt; order.markRefunded(); }
    public void refundFailed(String id, String reason) { if (!"PROCESSED".equals(refundStatus)) { if (id != null && !id.isBlank()) refundId = id; refundStatus = "FAILED"; refundFailureReason = reason == null ? "Refund failed" : reason.substring(0, Math.min(reason.length(), 500)); updatedAt = Instant.now(); } }
    public void setProviderEventId(String value) { providerEventId = value; updatedAt = Instant.now(); }
}
