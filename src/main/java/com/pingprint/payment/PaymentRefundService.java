package com.pingprint.payment;

import com.pingprint.printjob.PrintOrder;
import com.pingprint.printjob.PrintOrderRepository;
import com.pingprint.printjob.PrintJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentRefundService {
    private final RazorpayService gateway;
    private final PrintOrderRepository orders;
    private final PaymentTransactionRepository transactions;
    private final PrintJobRepository jobs;
    public PaymentRefundService(RazorpayService gateway, PrintOrderRepository orders, PaymentTransactionRepository transactions, PrintJobRepository jobs) {
        this.gateway = gateway; this.orders = orders; this.transactions = transactions; this.jobs = jobs;
    }

    @Transactional
    public Map<String, Object> initiateByAdmin(UUID transactionId, String reason) {
        PaymentTransaction payment = transactions.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        var job = jobs.findByOrderId(payment.getOrderId())
            .orElseThrow(() -> new IllegalStateException("The order has no print job"));
        if (!"FAILED".equals(job.getStatus())) {
            throw new IllegalStateException("Refunds are allowed only after a definite print failure");
        }
        String safeReason = reason == null || reason.isBlank() ? "Print job failed" : reason.trim();
        return initiateForDefinitePrintFailure(payment.getOrderId(), safeReason);
    }

    @Transactional
    public Map<String, Object> initiateForDefinitePrintFailure(UUID orderId, String reason) {
        PrintOrder order = orders.findById(orderId).orElseThrow();
        PaymentTransaction payment = transactions.findByOrder_Id(orderId).orElseThrow();
        if (order.getStatus().equals("REFUNDED") || "PROCESSED".equals(payment.getRefundStatus())) return result(payment, order);
        if (payment.getRefundId() != null && !"FAILED".equals(payment.getRefundStatus())) return result(payment, order);
        if (!payment.getStatus().equals("CAPTURED") || payment.getProviderPaymentId() == null) {
            throw new IllegalStateException("Only a captured payment can be refunded");
        }
        order.markRefundPending(); orders.saveAndFlush(order);
        try {
            Map<String, Object> response = gateway.refund(payment.getProviderPaymentId(), payment.getAmountMinor(), reason);
            String refundId = String.valueOf(response.get("id"));
            if (refundId.isBlank() || refundId.equals("null")) throw new IllegalStateException("Razorpay returned an incomplete refund response");
            String providerStatus = String.valueOf(response.getOrDefault("status", "pending"));
            payment.refundRequested(refundId, payment.getAmountMinor(), reason);
            if ("processed".equalsIgnoreCase(providerStatus)) payment.refundProcessed(refundId, payment.getAmountMinor());
            transactions.save(payment); orders.save(order);
        } catch (RuntimeException error) {
            payment.refundFailed(payment.getRefundId(), error.getMessage());
            transactions.save(payment);
        }
        return result(payment, order);
    }

    private Map<String, Object> result(PaymentTransaction payment, PrintOrder order) {
        return Map.of("orderId", order.getId(), "orderStatus", order.getStatus(),
            "refundStatus", payment.getRefundStatus() == null ? "NOT_REQUESTED" : payment.getRefundStatus());
    }
}
