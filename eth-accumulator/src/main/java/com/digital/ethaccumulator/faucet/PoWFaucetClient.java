package com.digital.ethaccumulator.faucet;

import com.digital.ethaccumulator.pow.PoWSolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client for pk910.de PoW faucets using their WebSocket protocol.
 *
 * Flow:
 *   connect → getConfig → startSession → receive updateMining challenge
 *   → solve SHA-256 PoW → foundShare → receive sessionUpdate (balance)
 *   → claimRewards → receive claimTx → done
 */
@Slf4j
public class PoWFaucetClient implements FaucetClient {

    private static final long WEI_PER_ETH = 1_000_000_000_000_000_000L;
    private static final int MAX_WAIT_MINUTES = 8;

    private final String name;
    private final String network;
    private final String wsUrl;
    private final PoWSolver powSolver;
    private final ObjectMapper objectMapper;

    public PoWFaucetClient(String name, String network, String baseUrl, PoWSolver powSolver) {
        this.name = name;
        this.network = network;
        // Convert https:// → wss:// for WebSocket
        this.wsUrl = baseUrl
            .replaceAll("/$", "")
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/ws";
        this.powSolver = powSolver;
        this.objectMapper = new ObjectMapper();
    }

    @Override public String getName()    { return name; }
    @Override public String getNetwork() { return network; }

    @Override
    public boolean isAvailable() {
        // Always attempt — connectivity is verified during the WS handshake itself
        return true;
    }

    @Override
    public ClaimResult claim(String walletAddress, boolean dryRun) {
        if (dryRun) {
            log.info("[DRY RUN] Would claim from {} for wallet {}", name, walletAddress);
            return ClaimResult.success(BigDecimal.valueOf(0.05), "dry-run-tx", 0L);
        }

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<ClaimResult> result = new AtomicReference<>();
        AtomicReference<String> sessionId = new AtomicReference<>();
        AtomicBoolean miningInProgress = new AtomicBoolean(false);
        ExecutorService miner = Executors.newSingleThreadExecutor();

        try {
            WebSocketClient ws = new WebSocketClient(new URI(wsUrl)) {

                @Override
                public void onOpen(ServerHandshake h) {
                    log.info("Connected to {} — requesting config", name);
                    sendJson(Map.of("action", "getConfig"));
                }

                @Override
                public void onMessage(String raw) {
                    try {
                        JsonNode msg = objectMapper.readTree(raw);
                        String action = msg.path("action").asText("");
                        log.debug("[{}] received: {}", name, action);

                        switch (action) {
                            case "open" ->
                                // Some faucet versions send "open" first — request config
                                sendJson(Map.of("action", "getConfig"));

                            case "config" -> {
                                log.info("Config received — starting session for {}", walletAddress);
                                sendJson(Map.of(
                                    "action", "startSession",
                                    "targetAddr", walletAddress,
                                    "token", ""
                                ));
                            }

                            case "sessionInfo" -> {
                                String sid = msg.path("session").asText();
                                sessionId.set(sid);
                                log.info("Session started: {}", sid);
                            }

                            case "updateMining" -> {
                                if (!miningInProgress.compareAndSet(false, true)) {
                                    log.debug("Mining already in progress, skipping duplicate challenge");
                                    return;
                                }

                                String preimage  = msg.path("preimage").asText();
                                String params    = msg.path("params").asText(preimage);
                                int difficulty   = msg.path("difficulty").asInt(22);
                                String sid       = msg.path("session").asText();
                                if (sid != null && !sid.isBlank()) sessionId.set(sid);

                                log.info("Mining challenge received — difficulty={} session={}", difficulty, sessionId.get());

                                miner.submit(() -> {
                                    try {
                                        PoWSolver.PoWSolution sol = powSolver.solve(preimage, difficulty);
                                        long hashrate = sol.nonce() / Math.max(1, sol.durationMs() / 1000);
                                        log.info("Share found: nonce={} in {}ms", sol.nonce(), sol.durationMs());

                                        sendJson(Map.of(
                                            "action",   "foundShare",
                                            "session",  sessionId.get(),
                                            "nonce",    sol.nonce(),
                                            "params",   params,
                                            "hashrate", hashrate
                                        ));
                                    } catch (Exception e) {
                                        log.error("PoW mining error", e);
                                    } finally {
                                        miningInProgress.set(false);
                                    }
                                });
                            }

                            case "sessionUpdate" -> {
                                long balanceWei = msg.path("balance").asLong(0);
                                log.info("Balance update: {} wei ({} ETH)", balanceWei, weiToEth(balanceWei));

                                if (balanceWei > 0 && sessionId.get() != null) {
                                    log.info("Requesting claim for session {}", sessionId.get());
                                    sendJson(Map.of(
                                        "action",  "claimRewards",
                                        "session", sessionId.get()
                                    ));
                                }
                            }

                            case "claimTx" -> {
                                String tx = msg.path("tx").asText();
                                long amountWei = msg.path("amount").asLong(0);
                                BigDecimal eth = amountWei > 0
                                    ? weiToEth(amountWei)
                                    : BigDecimal.valueOf(0.05);

                                log.info("Claim TX received: {} — {} ETH", tx, eth);
                                result.set(ClaimResult.success(eth, tx, null));
                                done.countDown();
                            }

                            case "error" -> {
                                String err = msg.path("text").asText(
                                    msg.path("message").asText("Unknown faucet error"));
                                log.error("Faucet error from {}: {}", name, err);
                                String lower = err.toLowerCase();
                                if (lower.contains("rate") || lower.contains("limit") || lower.contains("already")) {
                                    result.set(ClaimResult.rateLimited());
                                } else {
                                    result.set(ClaimResult.failure(err));
                                }
                                done.countDown();
                            }

                            default -> log.debug("Unhandled action '{}' from {}", action, name);
                        }

                    } catch (Exception e) {
                        log.error("Error processing WS message: {}", raw, e);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.info("WS closed [{}]: {}", code, reason);
                    if (result.get() == null) {
                        result.set(ClaimResult.failure("Connection closed before claim: " + reason));
                        done.countDown();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    log.error("WS error on {}: {}", name, ex.getMessage());
                    result.set(ClaimResult.failure(ex.getMessage()));
                    done.countDown();
                }

                private void sendJson(Object payload) {
                    try {
                        String json = objectMapper.writeValueAsString(payload);
                        log.debug("[{}] sending: {}", name, json);
                        send(json);
                    } catch (Exception e) {
                        log.error("Failed to send WS message", e);
                    }
                }
            };

            log.info("Connecting to {}", wsUrl);
            ws.connectBlocking(30, TimeUnit.SECONDS);

            boolean completed = done.await(MAX_WAIT_MINUTES, TimeUnit.MINUTES);
            miner.shutdownNow();
            ws.close();

            if (!completed) {
                return ClaimResult.failure("Timed out after " + MAX_WAIT_MINUTES + "min — faucet did not respond with a claim");
            }

            return result.get() != null ? result.get() : ClaimResult.failure("No result received");

        } catch (Exception e) {
            log.error("Fatal error claiming from {}: {}", name, e.getMessage(), e);
            return ClaimResult.failure(e.getMessage());
        } finally {
            miner.shutdownNow();
        }
    }

    private BigDecimal weiToEth(long wei) {
        return BigDecimal.valueOf(wei)
            .divide(BigDecimal.valueOf(WEI_PER_ETH), 18, RoundingMode.HALF_UP);
    }
}
