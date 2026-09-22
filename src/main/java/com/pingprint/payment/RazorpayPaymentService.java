package com.pingprint.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingprint.printjob.PrintOrder;
import com.pingprint.printjob.PrintOrderRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;

@Service
public class RazorpayPaymentService {
    private static final String PROVIDER = "RAZORPAY";
    private final RazorpayService gateway;
    private final PrintOrderRepository orders;
    private final PaymentTransactionRepository transactions;
    private final JdbcTemplate db;
    private final ObjectMapper json;

    public RazorpayPaymentService(RazorpayService gateway, PrintOrderRepository orders, PaymentTransactionRepository transactions, JdbcTemplate db, ObjectMapper json) {
        this.gateway = gateway; this.orders = orders; this.transactions = transactions; this.db = db; this.json = json;
    }

    @Transactional
    public Map<String, Object> createOrder(UUID internalOrderId) {
        PrintOrder order = orders.findById(internalOrderId).orElseThrow(() -> new IllegalArgumentException("Print order not found"));
        if (order.getRazorpayOrderId() != null) return Map.of("id", order.getRazorpayOrderId(), "amount", order.getAmountMinor(), "currency", order.getCurrency());
        Map<String, Object> response = gateway.createOrder(order.getAmountMinor(), "print_" + order.getId());
        String providerOrderId = String.valueOf(response.get("id"));
        order.setRazorpayOrderId(providerOrderId);
        transactions.save(new PaymentTransaction(order, PROVIDER, providerOrderId, order.getAmountMinor()));
        return response;
    }

    @Transactional
    public Map<String, Object> verify(String providerOrderId, String paymentId, String signature) {
        if (!gateway.verifyPayment(providerOrderId, paymentId, signature)) throw new IllegalArgumentException("Invalid Razorpay payment signature");
        PrintOrder order = orders.findByRazorpayOrderId(providerOrderId).orElseThrow(() -> new IllegalArgumentException("Print order not found for Razorpay order"));
        PaymentTransaction transaction = transactions.findByProviderAndProviderPaymentId(PROVIDER, paymentId).orElseGet(() -> transactions.findByProviderAndProviderOrderId(PROVIDER, providerOrderId).orElseThrow(() -> new IllegalArgumentException("Payment transaction not found")));
        if (!transaction.getStatus().equals("CAPTURED")) { transaction.capture(paymentId); transactions.save(transaction); }
        if (!order.getStatus().equals("PAID")) { order.markPaid(); orders.save(order); }
        return Map.of("verified", true, "status", "captured", "orderId", order.getId());
    }

    @Transactional
    public void webhook(String payload) {
        try {
            JsonNode root = json.readTree(payload);
            String eventId = root.path("payload").path("payment").path("entity").path("id").asText(root.path("id").asText());
            String event = root.path("event").asText();
            if (eventId.isBlank() || !claimEvent(eventId, event)) return;
            JsonNode payment = root.path("payload").path("payment").path("entity");
            String providerOrderId = payment.path("order_id").asText();
            String paymentId = payment.path("id").asText();
            if (providerOrderId.isBlank()) return;
            PrintOrder order = orders.findByRazorpayOrderId(providerOrderId).orElse(null);
            if (order == null) return;
            PaymentTransaction transaction = transactions.findByProviderAndProviderPaymentId(PROVIDER, paymentId).orElseGet(() -> transactions.findByProviderAndProviderOrderId(PROVIDER, providerOrderId).orElseThrow());
            if (event.equals("payment.captured") || event.equals("order.paid")) { transaction.capture(paymentId); order.markPaid(); }
            if (event.equals("payment.failed")) transaction.fail(payment.path("error_description").asText("Payment failed"));
            transaction.setProviderEventId(eventId); transactions.save(transaction); orders.save(order);
        } catch (Exception error) { throw new IllegalArgumentException("Invalid Razorpay webhook payload", error); }
    }

    private boolean claimEvent(String eventId, String event) {
        return db.update("INSERT INTO webhook_events (id, provider, event_type) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING", eventId, PROVIDER, event) == 1;
    }
}