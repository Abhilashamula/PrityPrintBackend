package com.pingprint.payment;

import com.pingprint.printjob.PrintOrder;
import com.pingprint.printjob.PrintOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentRefundService {
    private final RazorpayService gateway;
    private final PrintOrderRepository orders;
    private final PaymentTransactionRepository transactions;
    public PaymentRefundService(RazorpayService gateway, PrintOrderRepository orders, PaymentTransactionRepository transactions) { this.gateway = gateway; this.orders = orders; this.transactions = transactions; }

    @Transactional
    public void initiateForDefinitePrintFailure(UUID orderId, String reason) {
        PrintOrder order = orders.findById(orderId).orElseThrow();
        PaymentTransaction payment = transactions.findByOrderId(orderId).orElseThrow();
        if (payment.getRefundId() != null || order.getStatus().equals("REFUND_PENDING") || order.getStatus().equals("REFUNDED")) return;
        if (!payment.getStatus().equals("CAPTURED") || payment.getProviderPaymentId() == null) return;
        order.markRefundPending(); orders.saveAndFlush(order);
        try {
            Map<String, Object> response = gateway.refund(payment.getProviderPaymentId(), payment.getAmountMinor(), reason);
            String refundId = String.valueOf(response.get("id"));
            if (refundId.isBlank() || refundId.equals("null")) throw new IllegalStateException("Razorpay returned an incomplete refund response");
            payment.refund(refundId, payment.getAmountMinor(), reason); transactions.save(payment); orders.save(order);
        } catch (RuntimeException error) {
            // REFUND_PENDING is intentionally retained for webhook/admin reconciliation.
        }
    }
}
