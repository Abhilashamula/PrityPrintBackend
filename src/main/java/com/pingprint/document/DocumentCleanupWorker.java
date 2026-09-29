package com.pingprint.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class DocumentCleanupWorker {
    private final DocumentRepository documents;
    private final DocumentService storage;
    private final long retentionHours;
    public DocumentCleanupWorker(DocumentRepository documents, DocumentService storage,
        @Value("${app.storage.retention-hours:24}") long retentionHours) {
        this.documents = documents; this.storage = storage; this.retentionHours = Math.max(1, retentionHours);
    }
    @Scheduled(fixedDelayString = "${app.storage.cleanup-delay-ms:3600000}")
    public void cleanup() {
        Instant cutoff = Instant.now().minus(retentionHours, ChronoUnit.HOURS);
        documents.findCleanupCandidates(cutoff).forEach(storage::deleteStoredFile);
    }
}
