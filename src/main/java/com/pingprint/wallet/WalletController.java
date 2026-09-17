package com.pingprint.wallet;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    private final WalletRepository wallets;
    private final WalletTransactionRepository transactions;
    public WalletController(WalletRepository wallets, WalletTransactionRepository transactions) { this.wallets = wallets; this.transactions = transactions; }

    @GetMapping
    public Map<String, Object> wallet(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        Wallet wallet = wallets.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        var history = transactions.findTop20ByWalletIdOrderByCreatedAtDesc(wallet.getId()).stream().map(t -> Map.of("type", t.getType(), "amountMinor", t.getAmountMinor(), "balanceAfterMinor", t.getBalanceAfterMinor(), "provider", t.getProvider() == null ? "" : t.getProvider(), "createdAt", t.getCreatedAt())).toList();
        return Map.of("balanceMinor", wallet.getBalanceMinor(), "currency", wallet.getCurrency(), "transactions", history);
    }
}
