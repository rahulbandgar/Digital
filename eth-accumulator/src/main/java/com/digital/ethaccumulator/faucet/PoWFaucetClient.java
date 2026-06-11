package com.digital.ethaccumulator.faucet;

import com.digital.ethaccumulator.pow.Argon2Solver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URI;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client for pk910 PoWFaucet (Sepolia/Holesky).
 *
 * Protocol (reverse-engineered from github.com/pk910/PoWFaucet):
 *   1. POST /api/startSession        {addr}        → session id
 *   2. GET  /api/getFaucetConfig?session=..&cliver=.. → argon2 params + difficulty + min claim
 *   3. WS   /ws/pow?session=..&cliver=..
 *   4. mine argon2 shares → send {action:foundShare,...}; balance accrues via updateBalance
 *   5. answer {action:verify} peer-verification requests
 *   6. when balance ≥ min → {action:closeSession} (session becomes CLAIMABLE)
 *   7. POST /api/claimReward          {session}    → ETH sent on-chain
 */
@Slf4j
public class PoWFaucetClient implements FaucetClient {

    private static final String CLIVER = "2.5.0";
    private static final BigDecimal WEI_PER_ETH = new BigDecimal("1000000000000000000");
    private static final long DEFAULT_MIN_CLAIM_WEI = 10_000_000_000_000_000L; // 0.01 ETH

    private final String name;
    private final String network;
    private final String baseUrl;   // https://sepolia-faucet.pk910.de
    private final String wsBase;     // wss://sepolia-faucet.pk910.de
    private final Argon2Solver solver;
    private final OkHttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    private final int mineThreads = Math.max(2, Runtime.getRuntime().availableProcessors());
    private final long maxMineMillis;

    public PoWFaucetClient(String name, String network, String baseUrl, Argon2Solver solver) {
        this(name, network, baseUrl, solver, TimeUnit.MINUTES.toMillis(6));
    }

    public PoWFaucetClient(String name, String network, String baseUrl, Argon2Solver solver, long maxMineMillis) {
        this.name = name;
        this.network = network;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.wsBase = this.baseUrl.replaceFirst("^http", "ws");
        this.solver = solver;
        this.maxMineMillis = maxMineMillis;
        this.http = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    }

    @Override public String getName()    { return name; }
    @Override public String getNetwork() { return network; }
    @Override public boolean isAvailable() { return true; }

    @Override
    public ClaimResult claim(String walletAddress, boolean dryRun) {
        if (dryRun) {
            log.info("[DRY RUN] Would claim from {} for {}", name, walletAddress);
            return ClaimResult.success(BigDecimal.valueOf(0.05), "dry-run-tx", 0L);
        }
        try {
            // 1. start session
            JsonNode session = post("/api/startSession?cliver=" + CLIVER,
                Map.of("addr", walletAddress));
            if (isFailed(session)) return failFrom(session);

            String sessionId = session.path("session").asText();
            String preimageB64 = session.path("modules").path("pow").path("preImage").asText(null);
            log.info("Session {} started on {}", sessionId, name);

            // 2. faucet config → argon2 params + difficulty (+ min claim)
            JsonNode cfg = get("/api/getFaucetConfig?cliver=" + CLIVER + "&session=" + sessionId);
            JsonNode pow = cfg.path("modules").path("pow");
            if (preimageB64 == null || preimageB64.isBlank()) {
                preimageB64 = pow.path("preImage").asText(null);
            }
            if (preimageB64 == null) {
                return ClaimResult.failure("No PoW preimage returned by faucet");
            }
            byte[] preimage = Base64.getDecoder().decode(preimageB64);

            JsonNode pp = pow.path("powParams");
            Argon2Solver.Params params = new Argon2Solver.Params(
                pp.path("t").asInt(0),
                pp.path("v").asInt(19),
                pp.path("i").asInt(4),
                pp.path("m").asInt(4096),
                pp.path("p").asInt(1),
                pp.path("l").asInt(16),
                pow.path("powDifficulty").asInt(11)
            );
            long minClaimWei = cfg.path("minClaim").asLong(DEFAULT_MIN_CLAIM_WEI);
            log.info("PoW params: type={} v={} t={} m={} p={} l={} difficulty={} minClaim={} wei",
                params.type(), params.version(), params.timeCost(), params.memoryCost(),
                params.parallelism(), params.keyLength(), params.difficulty(), minClaimWei);

            // 3-6. mine over WebSocket until balance ≥ min, then close session
            return mineAndClaim(sessionId, preimage, params, minClaimWei, walletAddress);

        } catch (Exception e) {
            log.error("Error claiming from {}: {}", name, e.getMessage(), e);
            return ClaimResult.failure(e.getMessage());
        }
    }

    private ClaimResult mineAndClaim(String sessionId, byte[] preimage,
                                     Argon2Solver.Params params, long minClaimWei,
                                     String wallet) throws Exception {
        final AtomicReference<BigInteger> balance = new AtomicReference<>(BigInteger.ZERO);
        final AtomicLong lastNonce = new AtomicLong(0);
        final AtomicBoolean miningStopped = new AtomicBoolean(false);
        final AtomicBoolean closed = new AtomicBoolean(false);
        final CountDownLatch finished = new CountDownLatch(1);
        final AtomicReference<ClaimResult> result = new AtomicReference<>();
        final String paramsStr = powParamsStr(params);
        final ExecutorService miners = Executors.newFixedThreadPool(mineThreads);
        final AtomicLong reqId = new AtomicLong(1);

        String url = wsBase + "/ws/pow?session=" + sessionId + "&cliver=" + CLIVER;
        log.info("Connecting WS {}", url);

        WebSocketClient ws = new WebSocketClient(new URI(url)) {
            @Override public void onOpen(ServerHandshake h) {
                log.info("WS open — starting {} miner threads (difficulty {})", mineThreads, params.difficulty());
                startMiners(this);
            }

            @Override public void onMessage(String raw) {
                try { handle(mapper.readTree(raw), this); }
                catch (Exception e) { log.error("WS message error: {}", raw, e); }
            }

            @Override public void onClose(int code, String reason, boolean remote) {
                log.info("WS closed [{}] {}", code, reason);
                if (result.get() == null) {
                    result.set(ClaimResult.failure("WS closed before claim: " + code + " " + reason));
                    finished.countDown();
                }
            }

            @Override public void onError(Exception ex) {
                log.error("WS error: {}", ex.getMessage());
            }

            private void startMiners(WebSocketClient client) {
                AtomicLong nonceCursor = new AtomicLong(0);
                AtomicLong hashes = new AtomicLong(0);
                for (int t = 0; t < mineThreads; t++) {
                    miners.submit(() -> {
                        while (!miningStopped.get()) {
                            long base = nonceCursor.getAndAdd(1024);
                            for (long n = base; n < base + 1024 && !miningStopped.get(); n++) {
                                String hex = solver.hash(n, preimage, params);
                                hashes.incrementAndGet();
                                if (solver.meetsDifficulty(hex, solver.difficultyMask(params.difficulty()))) {
                                    submitShare(client, n, paramsStr, hashes.get());
                                }
                            }
                        }
                    });
                }
            }

            private synchronized void submitShare(WebSocketClient client, long nonce, String pstr, long hashCount) {
                if (miningStopped.get()) return;
                send(json(Map.of(
                    "id", reqId.getAndIncrement(),
                    "action", "foundShare",
                    "data", Map.of("nonce", nonce, "data", "", "params", pstr, "hashrate", 0)
                )));
            }

            private void handle(JsonNode msg, WebSocketClient client) {
                String action = msg.path("action").asText("");
                switch (action) {
                    case "updateBalance" -> {
                        String bal = msg.path("data").path("balance").asText("0");
                        balance.set(new BigInteger(bal.isEmpty() ? "0" : bal));
                        log.info("Balance: {} ETH ({} wei)", weiToEth(balance.get()), balance.get());
                        if (balance.get().compareTo(BigInteger.valueOf(minClaimWei)) >= 0
                                && miningStopped.compareAndSet(false, true)) {
                            log.info("Min claim reached — stopping miners and closing session");
                            miners.shutdownNow();
                            send(json(Map.of("id", reqId.getAndIncrement(), "action", "closeSession")));
                            closed.set(true);
                        }
                    }
                    case "verify" -> {
                        JsonNode d = msg.path("data");
                        long n = d.path("nonce").asLong();
                        String pre = d.path("preimage").asText();
                        byte[] vpre = Base64.getDecoder().decode(pre);
                        String hex = solver.hash(n, vpre, params);
                        boolean valid = solver.meetsDifficulty(hex, solver.difficultyMask(params.difficulty()));
                        send(json(Map.of(
                            "action", "verifyResult",
                            "data", Map.of("shareId", d.path("shareId").asText(),
                                           "params", paramsStr, "isValid", valid)
                        )));
                    }
                    case "ok" -> {
                        // response to closeSession → session now claimable
                        if (closed.get() && result.get() == null) {
                            ClaimResult cr = claimReward(sessionId);
                            result.set(cr);
                            finished.countDown();
                            client.close();
                        }
                    }
                    case "error" -> {
                        String txt = msg.path("data").path("message").asText(
                            msg.path("data").path("code").asText("error"));
                        log.warn("Faucet message error: {}", txt);
                        // share-level errors are non-fatal; only fail if nothing else resolves
                    }
                    default -> log.debug("WS action '{}' ({})", action, name);
                }
            }
        };

        ws.connectBlocking(30, TimeUnit.SECONDS);
        boolean ok = finished.await(maxMineMillis + TimeUnit.MINUTES.toMillis(2), TimeUnit.MILLISECONDS);
        miningStopped.set(true);
        miners.shutdownNow();

        if (!ok && result.get() == null) {
            // ran out of time — try claiming whatever accrued if above min
            if (balance.get().compareTo(BigInteger.valueOf(minClaimWei)) >= 0) {
                try { ws.send(json(Map.of("action", "closeSession"))); Thread.sleep(2000); } catch (Exception ignore) {}
                ClaimResult cr = claimReward(sessionId);
                ws.close();
                return cr;
            }
            ws.close();
            return ClaimResult.failure("Timed out mining; balance "
                + weiToEth(balance.get()) + " ETH below min " + weiToEth(BigInteger.valueOf(minClaimWei)));
        }
        return result.get() != null ? result.get() : ClaimResult.failure("No result");
    }

    private ClaimResult claimReward(String sessionId) {
        try {
            log.info("Claiming reward for session {}", sessionId);
            JsonNode resp = post("/api/claimReward", Map.of("session", sessionId));
            if (isFailed(resp)) return failFrom(resp);

            String bal = resp.path("balance").asText("0");
            BigDecimal eth = weiToEth(new BigInteger(bal.isEmpty() ? "0" : bal));
            String txHash = resp.path("claimIdx").asText(null); // claim is queued; tx confirmed async
            log.info("Claim queued — {} ETH, status={}", eth, resp.path("claimStatus").asText("queued"));
            return ClaimResult.success(eth, txHash, null);
        } catch (Exception e) {
            return ClaimResult.failure("claimReward failed: " + e.getMessage());
        }
    }

    // ---- faucet param string (must match getPoWParamsStr for ARGON2) ----
    private String powParamsStr(Argon2Solver.Params p) {
        // "a|t|v|i|m|p|l|difficulty" where a = "a2" (ARGON2)
        return "a2|" + p.type() + "|" + p.version() + "|" + p.timeCost() + "|"
            + p.memoryCost() + "|" + p.parallelism() + "|" + p.keyLength() + "|" + p.difficulty();
    }

    // ---- HTTP helpers ----
    private JsonNode post(String path, Object body) throws Exception {
        Request req = new Request.Builder()
            .url(baseUrl + path)
            .post(RequestBody.create(json(body), MediaType.parse("application/json")))
            .build();
        try (Response r = http.newCall(req).execute()) {
            return mapper.readTree(r.body() != null ? r.body().string() : "{}");
        }
    }

    private JsonNode get(String path) throws Exception {
        Request req = new Request.Builder().url(baseUrl + path).get().build();
        try (Response r = http.newCall(req).execute()) {
            return mapper.readTree(r.body() != null ? r.body().string() : "{}");
        }
    }

    private String json(Object o) {
        try { return mapper.writeValueAsString(o); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private boolean isFailed(JsonNode n) {
        return "failed".equalsIgnoreCase(n.path("status").asText(""));
    }

    private ClaimResult failFrom(JsonNode n) {
        String reason = n.path("failedReason").asText(n.path("failedCode").asText("unknown"));
        if (reason.toLowerCase().contains("limit") || reason.toLowerCase().contains("already")) {
            return ClaimResult.rateLimited();
        }
        return ClaimResult.failure(reason);
    }

    private BigDecimal weiToEth(BigInteger wei) {
        return new BigDecimal(wei).divide(WEI_PER_ETH, 18, RoundingMode.HALF_UP);
    }
}
