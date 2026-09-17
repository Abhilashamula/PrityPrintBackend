package com.pingprint.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Service
public class RazorpayService {
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final RestClient client;

    public RazorpayService(@Value("${app.razorpay.key-id}") String keyId, @Value("${app.razorpay.key-secret}") String keySecret, @Value("${app.razorpay.webhook-secret}") String webhookSecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.client = RestClient.builder().baseUrl("https://api.razorpay.com/v1").build();
    }

    public Map<String, Object> createOrder(long amountMinor, String receipt) {
        requireConfigured();
        return client.post().uri("/orders").headers(headers -> headers.setBasicAuth(keyId, keySecret))
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("amount", amountMinor, "currency", "INR", "receipt", receipt, "payment_capture", 1))
            .retrieve().body(Map.class);
    }

    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        requireConfigured();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).equals(signature);
        } catch (Exception error) {
            throw new IllegalStateException("Could not verify Razorpay payment", error);
        }
    }

    public boolean verifyWebhook(String payload, String signature) {
        if (webhookSecret.isBlank()) throw new IllegalStateException("Razorpay webhook secret is not configured");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).equals(signature);
        } catch (Exception error) {
            throw new IllegalStateException("Could not verify Razorpay webhook", error);
        }
    }

    private void requireConfigured() {
        if (keyId.isBlank() || keySecret.isBlank()) throw new IllegalStateException("Razorpay server credentials are not configured");
    }
}
