package com.pingprint.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingprint.printjob.PrintJobService;
import com.pingprint.printjob.PrintOrder;
import com.pingprint.printjob.PrintOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RazorpayPaymentServiceTest {
    private final RazorpayService gateway = mock(RazorpayService.class);
    private final PrintOrderRepository orders = mock(PrintOrderRepository.class);
    private final PaymentTransactionRepository transactions = mock(PaymentTransactionRepository.class);
    private final PrintJobService printJobs = mock(PrintJobService.class);
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final RazorpayPaymentService service =
        new RazorpayPaymentService(gateway, orders, transactions, printJobs, db, new ObjectMapper());

    @Test void duplicateCapturedWebhookEnqueuesOnlyOnce() {
        UUID orderId = UUID.randomUUID();
        PrintOrder order = mock(PrintOrder.class);
        PaymentTransaction transaction = mock(PaymentTransaction.class);
        when(order.getId()).thenReturn(orderId);
        when(order.getCurrency()).thenReturn("INR");
        when(transaction.getProviderOrderId()).thenReturn("order_1");
        when(transaction.getOrderId()).thenReturn(orderId);
        when(transaction.getAmountMinor()).thenReturn(500L);
        when(orders.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(order));
        when(transactions.findByProviderAndProviderPaymentId("RAZORPAY", "pay_1")).thenReturn(Optional.of(transaction));
        when(db.update(anyString(), any(), any(), any())).thenReturn(1, 0);
        String payload = """
            {"id":"evt_1","event":"payment.captured","payload":{"payment":{"entity":{
              "id":"pay_1","order_id":"order_1","amount":500,"currency":"INR"
            }}}}
            """;

        service.webhook(payload);
        service.webhook(payload);

        verify(transaction, times(1)).capture("pay_1");
        verify(printJobs, times(1)).enqueueOnce(order);
    }

    @Test void browserVerificationDoesNotCaptureOrEnqueue() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PrintOrder order = mock(PrintOrder.class);
        PaymentTransaction transaction = mock(PaymentTransaction.class);
        when(gateway.verifyPayment("order_1", "pay_1", "signature")).thenReturn(true);
        when(order.getId()).thenReturn(orderId);
        when(order.getUserId()).thenReturn(userId);
        when(transaction.getProviderOrderId()).thenReturn("order_1");
        when(transaction.getOrderId()).thenReturn(orderId);
        when(transaction.getStatus()).thenReturn("CREATED");
        when(orders.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(order));
        when(transactions.findByProviderAndProviderPaymentId("RAZORPAY", "pay_1")).thenReturn(Optional.empty());
        when(transactions.findByProviderAndProviderOrderId("RAZORPAY", "order_1")).thenReturn(Optional.of(transaction));

        Map<String, Object> response = service.verify("order_1", "pay_1", "signature", userId);

        assertThat(response.get("status")).isEqualTo("pending_confirmation");
        verify(transaction).linkPayment("pay_1");
        verify(transaction, never()).capture(anyString());
        verifyNoInteractions(printJobs);
    }

    @Test void guestCanVerifyGuestOrder() {
        UUID orderId = UUID.randomUUID();
        PrintOrder order = mock(PrintOrder.class);
        PaymentTransaction transaction = mock(PaymentTransaction.class);
        when(gateway.verifyPayment("order_guest", "pay_guest", "signature")).thenReturn(true);
        when(order.getId()).thenReturn(orderId);
        when(order.getUserId()).thenReturn(null);
        when(transaction.getProviderOrderId()).thenReturn("order_guest");
        when(transaction.getOrderId()).thenReturn(orderId);
        when(transaction.getStatus()).thenReturn("CREATED");
        when(orders.findByRazorpayOrderId("order_guest")).thenReturn(Optional.of(order));
        when(transactions.findByProviderAndProviderPaymentId("RAZORPAY", "pay_guest")).thenReturn(Optional.empty());
        when(transactions.findByProviderAndProviderOrderId("RAZORPAY", "order_guest")).thenReturn(Optional.of(transaction));

        Map<String, Object> response = service.verify("order_guest", "pay_guest", "signature", null);

        assertThat(response.get("verified")).isEqualTo(true);
    }

    @Test void duplicateRefundWebhookIsProcessedOnce() {
        UUID orderId = UUID.randomUUID();
        PrintOrder order = mock(PrintOrder.class);
        PaymentTransaction transaction = mock(PaymentTransaction.class);
        when(transaction.getOrderId()).thenReturn(orderId);
        when(transaction.getAmountMinor()).thenReturn(500L);
        when(transactions.findByRefundId("rfnd_1")).thenReturn(Optional.empty());
        when(transactions.findByProviderAndProviderPaymentId("RAZORPAY", "pay_1")).thenReturn(Optional.of(transaction));
        when(orders.findById(orderId)).thenReturn(Optional.of(order));
        when(db.update(anyString(), any(), any(), any())).thenReturn(1, 0);
        String payload = """
            {"id":"evt_refund_1","event":"refund.processed","payload":{"refund":{"entity":{
              "id":"rfnd_1","payment_id":"pay_1","amount":500,"currency":"INR","status":"processed"
            }}}}
            """;

        service.webhook(payload);
        service.webhook(payload);

        verify(transaction, times(1)).refundProcessed("rfnd_1", 500L);
    }
}
