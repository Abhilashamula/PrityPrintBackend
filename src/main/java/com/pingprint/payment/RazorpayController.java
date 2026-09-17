package com.pingprint.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/razorpay")
public class RazorpayController {
    private final RazorpayService razorpay;

    public RazorpayController(RazorpayService razorpay) { this.razorpay = razorpay; }

    public record CreateOrderRequest(@Min(100) long amountMinor, @NotBlank String receipt) { }
    public record VerifyPaymentRequest(@NotBlank String orderId, @NotBlank String paymentId, @NotBlank String signature) { }

    @PostMapping("/create-order")
    public Map<String, Object> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return razorpay.createOrder(request.amountMinor(), request.receipt());
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@Valid @RequestBody VerifyPaymentRequest request) {
        if (!razorpay.verifyPayment(request.orderId(), request.paymentId(), request.signature())) {
            throw new IllegalArgumentException("Invalid Razorpay payment signature");
        }
        return Map.of("verified", true, "status", "captured");
    }

    @PostMapping("/webhook")
    public Map<String, Object> webhook(@RequestBody String payload, @RequestHeader("X-Razorpay-Signature") String signature) {
        if (!razorpay.verifyWebhook(payload, signature)) throw new IllegalArgumentException("Invalid Razorpay webhook signature");
        return Map.of("received", true);
    }
}
