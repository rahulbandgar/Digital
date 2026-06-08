package com.digital.ethaccumulator.faucet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Client for API-key based faucets (Alchemy, QuickNode).
 * These accept a simple POST with wallet address + API key.
 */
@Slf4j
public class ApiKeyFaucetClient implements FaucetClient {

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

                if (resp.code() == 429) {
                    return ClaimResult.rateLimited();
                }

                if (!resp.isSuccessful()) {
                    return ClaimResult.failure("HTTP " + resp.code() + ": " + responseBody);
                }

                JsonNode json = objectMapper.readTree(responseBody);
                String txHash = json.path("txHash").asText(json.path("transactionHash").asText(null));
                BigDecimal amount = new BigDecimal(json.path("amount").asText("0.1"));

                log.info("Claimed {} ETH from {} — tx: {}", amount, name, txHash);
                return ClaimResult.success(amount, txHash, null);
            }

        } catch (Exception e) {
            log.error("Error claiming from {}: {}", name, e.getMessage(), e);
            return ClaimResult.failure(e.getMessage());
        }
    }
}
