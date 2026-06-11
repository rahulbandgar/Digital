package com.digital.ethaccumulator.faucet;

import com.digital.ethaccumulator.config.AppConfig;
import com.digital.ethaccumulator.pow.PoWSolver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FaucetRegistry {

    private final AppConfig appConfig;
    private final PoWSolver powSolver;

    private final List<FaucetClient> clients = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (appConfig.getFaucets() == null) {
            log.warn("No faucets configured in accumulator.faucets");
            return;
        }

        for (AppConfig.FaucetConfig cfg : appConfig.getFaucets()) {
            if (!cfg.isEnabled()) continue;

            FaucetClient client = switch (cfg.getType()) {
                case "pow" -> new PoWFaucetClient(cfg.getName(), appConfig.getNetwork(),
                    cfg.getUrl(), powSolver);
                case "api" -> new ApiKeyFaucetClient(cfg.getName(), appConfig.getNetwork(),
                    cfg.getUrl(), cfg.getApiKey());
                default -> throw new IllegalArgumentException("Unknown faucet type: " + cfg.getType());
            };

            clients.add(client);
            log.info("Registered faucet: {} [{}]", cfg.getName(), cfg.getType());
        }
    }

    public List<FaucetClient> getAll() {
        return Collections.unmodifiableList(clients);
    }
}
