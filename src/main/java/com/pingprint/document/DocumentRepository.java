package com.pingprint.document;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Optional<Document> findByIdAndUserId(UUID id, UUID userId);
    @Query(value = """
        SELECT d.* FROM documents d
        WHERE d.deleted_at IS NULL AND d.created_at < :cutoff
          AND EXISTS (SELECT 1 FROM print_orders o WHERE o.document_id=d.id
            AND o.status IN ('COMPLETED','PRINT_FAILED','PRINT_STATUS_UNKNOWN','REFUNDED','CANCELLED','FAILED'))
          AND NOT EXISTS (SELECT 1 FROM print_orders o JOIN print_jobs j ON j.order_id=o.id
            WHERE o.document_id=d.id AND j.status IN ('QUEUED','SUBMITTING','SUBMITTED','PRINTING'))
        ORDER BY d.created_at LIMIT 100
        """, nativeQuery = true)
    List<Document> findCleanupCandidates(Instant cutoff);
}
