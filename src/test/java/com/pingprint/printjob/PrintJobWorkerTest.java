package com.pingprint.printjob;

import com.pingprint.payment.PaymentRefundService;
import com.pingprint.printprovider.PrintProvider;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;

class PrintJobWorkerTest {
    @Test void definiteFailureReportedByProviderStartsRefund() {
        PrintJobClaimService claims = mock(PrintJobClaimService.class);
        PrintJobRepository jobs = mock(PrintJobRepository.class);
        PaymentRefundService refunds = mock(PaymentRefundService.class);
        PrintProvider provider = mock(PrintProvider.class);
        PrintJob job = mock(PrintJob.class);
        PrintOrder order = mock(PrintOrder.class);
        UUID orderId = UUID.randomUUID();
        when(provider.providerName()).thenReturn("EPSON_CONNECT");
        when(job.getProvider()).thenReturn("EPSON_CONNECT");
        when(job.getStatus()).thenReturn("FAILED");
        when(job.getFailureReason()).thenReturn("Paper jam");
        when(job.getOrder()).thenReturn(order);
        when(order.getId()).thenReturn(orderId);
        when(jobs.findTop20ByStatusInOrderByUpdatedAtAsc(any())).thenReturn(List.of(job));
        PrintJobWorker worker = new PrintJobWorker(claims, jobs, List.of(provider), refunds, 3);

        worker.pollSubmitted();

        verify(provider).refreshStatus(job);
        verify(refunds).initiateForDefinitePrintFailure(orderId, "Paper jam");
    }
}
