package com.digital.ethaccumulator.pow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Solves SHA-256 Proof-of-Work challenges used by pk910.de PoW faucets.
 * Challenge: find nonce N such that SHA256(challenge + N) starts with `difficulty` zero bits.
 */
@Slf4j
@Component
public class PoWSolver {

    public PoWSolution solve(String challenge, int difficulty) {
        log.info("Starting PoW solve: challenge={} difficulty={}", challenge, difficulty);
        long start = System.currentTimeMillis();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long nonce = 0;

            while (true) {
                String input = challenge + nonce;
                byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

                if (meetsTarget(hash, difficulty)) {
                    long duration = System.currentTimeMillis() - start;
                    log.info("PoW solved in {}ms, nonce={}", duration, nonce);
                    return new PoWSolution(nonce, bytesToHex(hash), duration);
                }

                nonce++;
                digest.reset();

                if (nonce % 1_000_000 == 0) {
                    log.debug("PoW progress: {} MH tried, elapsed {}ms", nonce / 1_000_000,
                        System.currentTimeMillis() - start);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private boolean meetsTarget(byte[] hash, int difficulty) {
        int fullBytes = difficulty / 8;
        int remainingBits = difficulty % 8;

        for (int i = 0; i < fullBytes; i++) {
            if (hash[i] != 0) return false;
        }

        if (remainingBits > 0) {
            int mask = 0xFF << (8 - remainingBits);
            return (hash[fullBytes] & mask) == 0;
        }

        return true;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public record PoWSolution(long nonce, String hash, long durationMs) {}
}
