package com.pingprint.printjob;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
public class PrintJobClaimService {
    private final PrintJobRepository jobs;
    public PrintJobClaimService(PrintJobRepository jobs) { this.jobs = jobs; }
    @Transactional
    public Optional<UUID> claim(String workerId) {
        return jobs.lockNextQueued().map(job -> { job.claim(workerId); jobs.save(job); return job.getId(); });
    }
    @Transactional
    public void recoverStale() { jobs.quarantineStaleSubmissions(); }
}
