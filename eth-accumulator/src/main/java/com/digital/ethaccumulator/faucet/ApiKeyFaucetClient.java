package com.digital.ethaccumulator.faucet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

/**
 * Client for API-key based faucets (Chainstack, Alchemy, QuickNode).
 * These accept a simple POST with wallet address, authenticated by an API key.
 *
 * Chainstack: POST https://api.chainstack.com/v1/faucet/sepolia
 *   Authorization: Bearer <key>; body {"address": "0x..."}
 *   response: {"amountSent": <wei>, "transaction": "https://sepolia.etherscan.io/tx/0x..."}
 */
@Slf4j
public class ApiKeyFaucetClient implements FaucetClient {

    private static final BigDecimal WEI_PER_ETH = new BigDecimal("1000000000000000000");

    private final String name;
    private final String network;
    private final String endpoint;
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiKeyFaucetClient(String name, String network, String endpoint, String apiKey) {
        this.name = name;
        this.network = network;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getNetwork() { return network; }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public ClaimResult claim(String walletAddress, boolean dryRun) {
        if (dryRun) {
            log.info("[DRY RUN] Would claim from {} for wallet {}", name, walletAddress);
            return ClaimResult.success(BigDecimal.valueOf(0.1), "dry-run-tx", null);
        }

        try {
            log.info("Claiming from {} for wallet {}", name, walletAddress);

            String body = objectMapper.writeValueAsString(
                java.util.Map.of("address", walletAddress)
            );

            Request req = new Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body, MediaType.parse("application/json")))
                .build();

            try (Response resp = httpClient.newCall(req).execute()) {
                String responseBody = resp.body() != null ? resp.body().string() : "";
                log.info("{} response [{}]: {}", name, resp.code(), responseBody);

                if (resp.code() == 429) {
                    return ClaimResult.rateLimited();
                }

                if (!resp.isSuccessful()) {
                    String lower = responseBody.toLowerCase();
                    if (lower.contains("24 hour") || lower.contains("already") || lower.contains("rate")) {
                        return ClaimResult.rateLimited();
                    }
                    return ClaimResult.failure("HTTP " + resp.code() + ": " + responseBody);
                }

                JsonNode json = objectMapper.readTree(responseBody);

                // tx hash: Chainstack returns a "transaction" URL; others use txHash/transactionHash
                String txHash = firstNonBlank(
                    json.path("txHash").asText(null),
                    json.path("transactionHash").asText(null),
                    extractTxHash(json.path("transaction").asText(null))
                );

                // amount: Chainstack returns amountSent in wei; others may return decimal "amount"
                BigDecimal amount;
                if (json.has("amountSent")) {
                    amount = new BigDecimal(json.path("amountSent").asText("0"))
                        .divide(WEI_PER_ETH, 18, RoundingMode.HALF_UP);
                } else {
                    amount = new BigDecimal(json.path("amount").asText("0.5"));
                }

                log.info("Claimed {} ETH from {} — tx: {}", amount, name, txHash);
                return ClaimResult.success(amount, txHash, null);
            }

        } catch (Exception e) {
            log.error("Error claiming from {}: {}", name, e.getMessage(), e);
            return ClaimResult.failure(e.getMessage());
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank() && !"null".equals(v)) return v;
        }
        return null;
    }

    /** Pull the 0x… tx hash out of an etherscan URL if that's what the faucet returned. */
    private static String extractTxHash(String maybeUrl) {
        if (maybeUrl == null) return null;
        int idx = maybeUrl.indexOf("0x");
        return idx >= 0 ? maybeUrl.substring(idx) : maybeUrl;
    }
}
