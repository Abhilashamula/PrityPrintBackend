package com.pingprint.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/api/payments/razorpay")
public class RazorpayController {
    private final RazorpayService razorpay;
    private final RazorpayPaymentService payments;

    public RazorpayController(RazorpayService razorpay, RazorpayPaymentService payments) { this.razorpay = razorpay; this.payments = payments; }

    public record VerifyPaymentRequest(@NotBlank String orderId, @NotBlank String paymentId, @NotBlank String signature) { }
    public record CreatePrintOrderPaymentRequest(UUID printOrderId) { }

    @PostMapping("/create-order-for-print")
    public Map<String, Object> createOrderForPrint(@Valid @RequestBody CreatePrintOrderPaymentRequest request, Authentication authentication) {
        if (request.printOrderId() == null) throw new IllegalArgumentException("Print order ID is required");
        return payments.createOrder(request.printOrderId(), userId(authentication));
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@Valid @RequestBody VerifyPaymentRequest request, Authentication authentication) {
        return payments.verify(request.orderId(), request.paymentId(), request.signature(), userId(authentication));
    }

    @PostMapping("/webhook")
    public Map<String, Object> webhook(@RequestBody String payload, @RequestHeader("X-Razorpay-Signature") String signature) {
        if (!razorpay.verifyWebhook(payload, signature)) throw new IllegalArgumentException("Invalid Razorpay webhook signature");
        payments.webhook(payload);
        return Map.of("received", true);
    }
    private UUID userId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UUID id) return id;
        throw new AccessDeniedException("Sign in is required");
    }
}
