package com.pingprint.printjob;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.UUID;

public interface PrintJobRepository extends JpaRepository<PrintJob, UUID> {
    Optional<PrintJob> findByOrderId(UUID orderId);
    @Query(value = "SELECT * FROM print_jobs WHERE status = 'QUEUED' AND next_attempt_at <= NOW() ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1", nativeQuery = true)
    Optional<PrintJob> lockNextQueued();
    @EntityGraph(attributePaths = {"order", "order.printer", "order.document"})
    java.util.List<PrintJob> findTop20ByStatusInOrderByUpdatedAtAsc(java.util.Collection<String> statuses);
    @EntityGraph(attributePaths = {"order", "order.printer", "order.document"})
    @Query("select j from PrintJob j where j.id = :id")
    Optional<PrintJob> findDetailedById(UUID id);
    @Modifying
    @Query(value = "UPDATE print_jobs SET status = 'STATUS_UNKNOWN', failure_reason = 'Worker stopped during provider submission; verify provider state before retrying', updated_at = NOW() WHERE status = 'SUBMITTING' AND claimed_at < NOW() - INTERVAL '10 minutes'", nativeQuery = true)
    int quarantineStaleSubmissions();
}
