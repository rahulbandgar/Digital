package com.digital.ethaccumulator.faucet;

import java.math.BigDecimal;

public interface FaucetClient {

    String getName();

    String getNetwork();

    boolean isAvailable();

    ClaimResult claim(String walletAddress, boolean dryRun);

    record ClaimResult(
        boolean success,
        BigDecimal ethAmount,
        String txHash,
        String errorMessage,
        Long powSolveDurationMs
    ) {
        public static ClaimResult success(BigDecimal ethAmount, String txHash, Long powDurationMs) {
            return new ClaimResult(true, ethAmount, txHash, null, powDurationMs);
        }

        public static ClaimResult failure(String error) {
            return new ClaimResult(false, null, null, error, null);
        }

        public static ClaimResult rateLimited() {
            return new ClaimResult(false, null, null, "RATE_LIMITED", null);
        }
    }
}
