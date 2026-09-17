package com.pingprint.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    List<WalletTransaction> findTop20ByWalletIdOrderByCreatedAtDesc(UUID walletId);
}
