package com.pingprint.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments/razorpay")
public class RazorpayController {
    private final RazorpayService razorpay;
    private final RazorpayPaymentService payments;

    public RazorpayController(RazorpayService razorpay, RazorpayPaymentService payments) { this.razorpay = razorpay; this.payments = payments; }

    public record CreateOrderRequest(@Min(100) long amountMinor, @NotBlank String receipt) { }
    public record VerifyPaymentRequest(@NotBlank String orderId, @NotBlank String paymentId, @NotBlank String signature) { }
    public record CreatePrintOrderPaymentRequest(UUID printOrderId) { }

    @PostMapping("/create-order")
    public Map<String, Object> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return razorpay.createOrder(request.amountMinor(), request.receipt());
    }

    @PostMapping("/create-order-for-print")
    public Map<String, Object> createOrderForPrint(@Valid @RequestBody CreatePrintOrderPaymentRequest request) {
        if (request.printOrderId() == null) throw new IllegalArgumentException("Print order ID is required");
        return payments.createOrder(request.printOrderId());
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@Valid @RequestBody VerifyPaymentRequest request) {
        return payments.verify(request.orderId(), request.paymentId(), request.signature());
    }

    @PostMapping("/webhook")
    public Map<String, Object> webhook(@RequestBody String payload, @RequestHeader("X-Razorpay-Signature") String signature) {
        if (!razorpay.verifyWebhook(payload, signature)) throw new IllegalArgumentException("Invalid Razorpay webhook signature");
        payments.webhook(payload);
        return Map.of("received", true);
    }
}
