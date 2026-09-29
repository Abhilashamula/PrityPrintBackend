package com.pingprint.printjob;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "print_jobs")
public class PrintJob {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false, unique = true) private PrintOrder order;
    @Column(nullable = false, length = 24) private String status;
    @Column(name = "pages_completed", nullable = false) private int pagesCompleted;
    @Column(name = "failure_reason") private String failureReason;
    @Column(length = 32) private String provider;
    @Column(name = "provider_job_id") private String providerJobId;
    @Column(name = "claimed_by") private String claimedBy;
    @Column(name = "claimed_at") private Instant claimedAt;
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "provider_status") private String providerStatus;
    @Column(name = "provider_status_reason") private String providerStatusReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected PrintJob() { }

    public PrintJob(PrintOrder order) {
        this.order = order;
        this.status = "QUEUED";
        this.pagesCompleted = 0;
        this.provider = order.getPrinter().getProvider();
        this.nextAttemptAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public UUID getId() { return id; }
    public PrintOrder getOrder() { return order; }
    public String getStatus() { return status; }
    public int getPagesCompleted() { return pagesCompleted; }
    public String getFailureReason() { return failureReason; }
    public String getProvider() { return provider; }
    public String getProviderJobId() { return providerJobId; }
    public Instant getClaimedAt() { return claimedAt; }
    public void claim(String workerId) { status = "SUBMITTING"; claimedBy = workerId; claimedAt = Instant.now(); startedAt = startedAt == null ? claimedAt : startedAt; attemptCount++; touch(); }
    public void recordProviderJob(String jobId) { providerJobId = jobId; touch(); }
    public void submitted() { status = "SUBMITTED"; failureReason = null; touch(); }
    public void printing(String providerStatus, String reason) { status = "PRINTING"; this.providerStatus = providerStatus; providerStatusReason = reason; order.markPrinting(); touch(); }
    public void completed() { status = "COMPLETED"; pagesCompleted = order.getTotalPages() * order.getCopies(); completedAt = Instant.now(); order.markCompleted(); touch(); }
    public void failed(String reason) { status = "FAILED"; failureReason = truncate(reason); completedAt = Instant.now(); order.markPrintFailed(); touch(); }
    public void unknown(String reason) { status = "STATUS_UNKNOWN"; failureReason = truncate(reason); order.markStatusUnknown(); touch(); }
    public void requeue(String reason, long delaySeconds) { status = "QUEUED"; failureReason = truncate(reason); claimedBy = null; claimedAt = null; nextAttemptAt = Instant.now().plusSeconds(delaySeconds); touch(); }
    public void providerStatus(String status, String reason) { providerStatus = status; providerStatusReason = truncate(reason); touch(); }
    private String truncate(String value) { return value == null ? null : value.substring(0, Math.min(value.length(), 255)); }
    private void touch() { updatedAt = Instant.now(); }
}
