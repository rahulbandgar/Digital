package com.digital.ethaccumulator.faucet;

import com.digital.ethaccumulator.pow.PoWSolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

/**
 * Client for pk910.de style PoW faucets (Sepolia, Holesky).
 * These faucets use a REST+PoW flow:
 *   1. POST /api/startSession → challenge + difficulty
 *   2. Solve SHA-256 PoW locally
 *   3. POST /api/claimReward  → tx hash + ETH amount
 */
@Slf4j
public class PoWFaucetClient implements FaucetClient {

    private static final long WEI_PER_ETH = 1_000_000_000_000_000_000L;

    private final String name;
    private final String network;
    private final String baseUrl;
    private final PoWSolver powSolver;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PoWFaucetClient(String name, String network, String baseUrl, PoWSolver powSolver) {
        this.name = name;
        this.network = network;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.powSolver = powSolver;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getNetwork() { return network; }

    @Override
    public boolean isAvailable() {
        try {
            Request req = new Request.Builder()
                .url(baseUrl + "/api/faucetConfig")
                .get()
                .build();
            try (Response resp = httpClient.newCall(req).execute()) {
                return resp.isSuccessful();
            }
        } catch (Exception e) {
            log.warn("Faucet {} health check failed: {}", name, e.getMessage());
            return false;
        }
    }

    @Override
    public ClaimResult claim(String walletAddress, boolean dryRun) {
        if (dryRun) {
            log.info("[DRY RUN] Would claim from {} for wallet {}", name, walletAddress);
            return ClaimResult.success(BigDecimal.valueOf(0.05), "dry-run-tx", 0L);
        }

        try {
            // Step 1: start session
            log.info("Starting PoW session on {} for {}", name, walletAddress);
            JsonNode session = startSession(walletAddress);

            if (session.has("error")) {
                String error = session.get("error").asText();
                if (error.contains("rate limit") || error.contains("too many")) {
                    return ClaimResult.rateLimited();
                }
                return ClaimResult.failure(error);
            }

            String sessionId = session.get("session").asText();
            String challenge = session.get("powChallenge").asText();
            int difficulty = session.get("powDifficulty").asInt(20);

            // Step 2: solve PoW
            PoWSolver.PoWSolution solution = powSolver.solve(challenge, difficulty);

            // Step 3: submit solution and claim
            log.info("Submitting PoW solution for session {}", sessionId);
            JsonNode result = submitSolution(sessionId, solution.nonce());

            if (result.has("error")) {
                return ClaimResult.failure(result.get("error").asText());
            }

            BigDecimal ethAmount = weiToEth(result.path("amount").asLong(0));
            String txHash = result.path("txHash").asText(null);

            log.info("Claimed {} ETH from {} — tx: {}", ethAmount, name, txHash);
            return ClaimResult.success(ethAmount, txHash, solution.durationMs());

        } catch (Exception e) {
            log.error("Error claiming from {}: {}", name, e.getMessage(), e);
            return ClaimResult.failure(e.getMessage());
        }
    }

    private JsonNode startSession(String walletAddress) throws Exception {
        String body = objectMapper.writeValueAsString(
            java.util.Map.of("address", walletAddress)
        );
        Request req = new Request.Builder()
            .url(baseUrl + "/api/startSession")
            .post(RequestBody.create(body, MediaType.parse("application/json")))
            .build();

        try (Response resp = httpClient.newCall(req).execute()) {
            return objectMapper.readTree(resp.body().string());
        }
    }

    private JsonNode submitSolution(String sessionId, long nonce) throws Exception {
        String body = objectMapper.writeValueAsString(
            java.util.Map.of("session", sessionId, "nonce", nonce)
        );
        Request req = new Request.Builder()
            .url(baseUrl + "/api/claimReward")
            .post(RequestBody.create(body, MediaType.parse("application/json")))
            .build();

        try (Response resp = httpClient.newCall(req).execute()) {
            return objectMapper.readTree(resp.body().string());
        }
    }

    private BigDecimal weiToEth(long wei) {
        return BigDecimal.valueOf(wei)
            .divide(BigDecimal.valueOf(WEI_PER_ETH), 18, RoundingMode.HALF_UP);
    }
}
