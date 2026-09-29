package com.pingprint.printjob;

import com.pingprint.printprovider.PrintProvider;
import com.pingprint.printprovider.ProviderException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.pingprint.payment.PaymentRefundService;
import org.springframework.beans.factory.annotation.Value;

@Service
public class PrintJobWorker {
    private final PrintJobClaimService claims;
    private final PrintJobRepository jobs;
    private final Map<String, PrintProvider> providers;
    private final String workerId;
    private final PaymentRefundService refunds;
    private final int maxAttempts;
    public PrintJobWorker(PrintJobClaimService claims, PrintJobRepository jobs, Collection<PrintProvider> providers, PaymentRefundService refunds,
                          @Value("${app.print-worker.max-attempts:3}") int maxAttempts) {
        this.claims = claims; this.jobs = jobs; this.providers = providers.stream().collect(Collectors.toMap(PrintProvider::providerName, Function.identity()));
        this.refunds = refunds;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.workerId = host() + "-" + UUID.randomUUID();
    }
    @Scheduled(fixedDelayString = "${app.print-worker.delay-ms:2000}")
    public void submitNext() { claims.claim(workerId).ifPresent(this::submit); }
    @Scheduled(fixedDelayString = "${app.print-worker.status-delay-ms:10000}")
    public void pollSubmitted() {
        for (PrintJob job : jobs.findTop20ByStatusInOrderByUpdatedAtAsc(java.util.List.of("SUBMITTED", "PRINTING"))) {
            PrintProvider provider = providers.get(job.getProvider());
            if (provider == null) continue;
            try { provider.refreshStatus(job); }
            catch (ProviderException error) { if (error.isAmbiguous()) { job.providerStatus("unavailable", error.getMessage()); jobs.save(job); } }
            catch (RuntimeException ignored) { }
        }
    }
    @Scheduled(fixedDelayString = "${app.print-worker.recovery-delay-ms:60000}")
    public void recover() { claims.recoverStale(); }
    private void submit(UUID id) {
        PrintJob job = jobs.findDetailedById(id).orElse(null); if (job == null) return;
        PrintProvider provider = providers.get(job.getProvider());
        if (provider == null) { job.failed("No print provider is registered for " + job.getProvider()); jobs.save(job); return; }
        try { provider.submit(job); }
        catch (IllegalStateException error) {
            if (job.getAttemptCount() >= maxAttempts) {
                job.failed("Submission could not start after " + maxAttempts + " attempts: " + error.getMessage());
                jobs.save(job); refunds.initiateForDefinitePrintFailure(job.getOrder().getId(), error.getMessage());
            } else {
                job.requeue(error.getMessage(), Math.min(300, 30L * job.getAttemptCount())); jobs.save(job);
            }
        }
        catch (ProviderException error) {
            if (error.isAmbiguous()) job.unknown(error.getMessage());
            else job.failed(error.getMessage());
            jobs.save(job);
            if (!error.isAmbiguous()) refunds.initiateForDefinitePrintFailure(job.getOrder().getId(), error.getMessage());
        }
        catch (RuntimeException error) { job.unknown("Unexpected provider error; verify provider state before retrying"); jobs.save(job); }
    }
    private String host() { try { return InetAddress.getLocalHost().getHostName(); } catch (Exception ignored) { return "worker"; } }
}
