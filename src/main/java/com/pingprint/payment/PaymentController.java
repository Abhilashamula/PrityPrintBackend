package com.pingprint.payment;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/paypal")
public class PaymentController {
    @PostMapping("/create-order")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, String> createOrder(Authentication authentication) {
        return Map.of("error", "PayPal is not configured yet. Add server-side PayPal credentials and implement order creation.");
    }

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, String> webhook(@RequestBody String payload, @RequestHeader(value = "PAYPAL-TRANSMISSION-ID", required = false) String transmissionId) {
        // Do not acknowledge or credit wallets until PayPal signature verification is implemented.
        return Map.of("error", "PayPal webhook verification is not configured yet.");
    }
}