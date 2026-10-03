package com.pingprint.payment;

import com.pingprint.printjob.PrintJob;
import com.pingprint.printjob.PrintJobRepository;
import com.pingprint.printjob.PrintOrder;
import com.pingprint.printjob.PrintOrderRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PaymentRefundServiceTest {
    private final RazorpayService gateway = mock(RazorpayService.class);
    private final PrintOrderRepository orders = mock(PrintOrderRepository.class);
    private final PaymentTransactionRepository transactions = mock(PaymentTransactionRepository.class);
    private final PrintJobRepository jobs = mock(PrintJobRepository.class);
    private final PaymentRefundService service = new PaymentRefundService(gateway, orders, transactions, jobs);

    @Test void adminCannotRefundAmbiguousPrintOutcome() {
        UUID transactionId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentTransaction payment = mock(PaymentTransaction.class);
        PrintJob job = mock(PrintJob.class);
        when(payment.getOrderId()).thenReturn(orderId);
        when(job.getStatus()).thenReturn("STATUS_UNKNOWN");
        when(transactions.findById(transactionId)).thenReturn(Optional.of(payment));
        when(jobs.findByOrderId(orderId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.initiateByAdmin(transactionId, "unknown"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("definite print failure");
        verifyNoInteractions(gateway);
    }

    @Test void alreadyProcessedRefundDoesNotCallGatewayAgain() {
        UUID orderId = UUID.randomUUID();
        PrintOrder order = mock(PrintOrder.class);
        PaymentTransaction payment = mock(PaymentTransaction.class);
        when(order.getId()).thenReturn(orderId);
        when(order.getStatus()).thenReturn("REFUNDED");
        when(payment.getRefundStatus()).thenReturn("PROCESSED");
        when(orders.findById(orderId)).thenReturn(Optional.of(order));
        when(transactions.findByOrder_Id(orderId)).thenReturn(Optional.of(payment));

        service.initiateForDefinitePrintFailure(orderId, "failed");

        verifyNoInteractions(gateway);
    }
}
