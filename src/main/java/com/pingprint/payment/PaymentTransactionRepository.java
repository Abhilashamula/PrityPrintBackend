package com.pingprint.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByProviderAndProviderPaymentId(String provider, String paymentId);
    Optional<PaymentTransaction> findByProviderAndProviderOrderId(String provider, String orderId);
    Optional<PaymentTransaction> findByOrderId(UUID orderId);
}
