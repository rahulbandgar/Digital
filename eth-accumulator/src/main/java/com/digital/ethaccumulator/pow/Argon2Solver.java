package com.digital.ethaccumulator.pow;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Argon2 Proof-of-Work solver for pk910 PoWFaucet (Sepolia/Holesky).
 *
 * Mirrors the faucet's hash_a2.c / worker-argon2 implementation exactly:
 *   hash = argon2(password = nonce(8 bytes), salt = preimage bytes, params)
 *   share is valid when hashHex.substring(0, mask.length) <= difficultyMask
 *
 * Default live params: Argon2id-style type field from server (0=d,1=i,2=id),
 * version 19, timeCost 4, memoryCost 4096 KB, parallelism 1, keyLength 16.
 */
@Slf4j
@Component
public class Argon2Solver {

    /** Argon2 params delivered by the faucet's getFaucetConfig (modules.pow.powParams). */
    public record Params(int type, int version, int timeCost, int memoryCost,
                         int parallelism, int keyLength, int difficulty) {}

    public record Solution(long nonce, String hashHex, long durationMs, long hashes) {}

    /**
     * Compute the Argon2 hash for a single nonce — used both for mining and for
     * answering peer verification requests.
     */
    public String hash(long nonce, byte[] preimage, Params p) {
        // password = nonce as 8-byte big-endian (matches 16-char hex → 8 bytes in hash_a2.c)
        byte[] password = new byte[8];
        for (int i = 7; i >= 0; i--) {
            password[i] = (byte) (nonce & 0xFF);
            nonce >>>= 8;
        }

        Argon2Parameters params = new Argon2Parameters.Builder(p.type())
            .withVersion(p.version())
            .withIterations(p.timeCost())
            .withMemoryAsKB(p.memoryCost())
            .withParallelism(p.parallelism())
            .withSalt(preimage)
            .build();

        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(params);
        byte[] out = new byte[p.keyLength()];
        gen.generateBytes(password, out);
        return toHex(out);
    }

    /** True if the hash satisfies the difficulty mask (string comparison, as the faucet does). */
    public boolean meetsDifficulty(String hashHex, String mask) {
        return hashHex.substring(0, mask.length()).compareTo(mask) <= 0;
    }

    /**
     * Build the difficulty mask exactly like the faucet:
     *   byteCount = floor(diff/8)+1; bitCount = diff - (byteCount-1)*8;
     *   maxValue  = 2^(8-bitCount); mask = hex(maxValue) left-padded to byteCount*2.
     */
    public String difficultyMask(int difficulty) {
        int byteCount = (difficulty / 8) + 1;
        int bitCount = difficulty - ((byteCount - 1) * 8);
        long maxValue = (long) Math.pow(2, 8 - bitCount);
        StringBuilder mask = new StringBuilder(Long.toHexString(maxValue));
        while (mask.length() < byteCount * 2) {
            mask.insert(0, '0');
        }
        return mask.toString();
    }

    /**
     * Mine starting from {@code startNonce}, stepping by {@code stride} (for multi-threading),
     * until a valid share is found or {@code stop} is set.
     */
    public Solution mine(byte[] preimage, Params p, long startNonce, long stride,
                         AtomicBoolean stop, AtomicLong sharedHashCounter) {
        String mask = difficultyMask(p.difficulty());
        long start = System.currentTimeMillis();
        long nonce = startNonce;
        long localHashes = 0;

        while (!stop.get()) {
            String hashHex = hash(nonce, preimage, p);
            localHashes++;

            if (meetsDifficulty(hashHex, mask)) {
                long dur = System.currentTimeMillis() - start;
                if (sharedHashCounter != null) sharedHashCounter.addAndGet(localHashes);
                return new Solution(nonce, hashHex, dur, localHashes);
            }
            nonce += stride;
        }
        if (sharedHashCounter != null) sharedHashCounter.addAndGet(localHashes);
        return null;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
