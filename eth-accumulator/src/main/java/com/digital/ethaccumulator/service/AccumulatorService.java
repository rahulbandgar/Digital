package com.digital.ethaccumulator.service;

import com.digital.ethaccumulator.config.AppConfig;
import com.digital.ethaccumulator.faucet.FaucetClient;
import com.digital.ethaccumulator.faucet.FaucetRegistry;
import com.digital.ethaccumulator.model.ClaimRecord;
import com.digital.ethaccumulator.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccumulatorService {

    private final AppConfig appConfig;
    private final FaucetRegistry faucetRegistry;
    private final ClaimRepository claimRepository;

    public List<ClaimRecord> runDailyClaims() {
        String wallet = appConfig.getWalletAddress();
        boolean dryRun = appConfig.isDryRun();

        if (wallet == null || wallet.isBlank()) {
            log.error("No wallet address configured. Set accumulator.wallet-address");
            return List.of();
        }

        log.info("=== Daily claim run starting — wallet={} dryRun={} ===", wallet, dryRun);
        List<ClaimRecord> results = new ArrayList<>();

        for (FaucetClient faucet : faucetRegistry.getAll()) {
            // Skip if already claimed from this faucet today
            if (alreadyClaimedToday(faucet.getName())) {
                log.info("Skipping {} — already claimed today", faucet.getName());
                results.add(buildRecord(faucet, ClaimRecord.ClaimStatus.SKIPPED, null));
                continue;
            }

            if (!dryRun && !faucet.isAvailable()) {
                log.warn("Faucet {} is not available, skipping", faucet.getName());
                results.add(buildRecord(faucet, ClaimRecord.ClaimStatus.FAILED, "Faucet unreachable"));
                continue;
            }

            FaucetClient.ClaimResult result = faucet.claim(wallet, dryRun);
            ClaimRecord record = toRecord(faucet, result, wallet);
            claimRepository.save(record);
            results.add(record);

            log.info("Faucet {}: status={} eth={}", faucet.getName(), record.getStatus(), record.getEthAmount());
        }

        log.info("=== Daily claim run complete — {} faucets processed ===", results.size());
        return results;
    }

    private boolean alreadyClaimedToday(String faucetName) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return claimRepository.existsByFaucetNameAndClaimedAtAfter(faucetName, startOfDay);
    }

    private ClaimRecord toRecord(FaucetClient faucet, FaucetClient.ClaimResult result, String wallet) {
        ClaimRecord.ClaimStatus status;
        if (result.success()) {
            status = ClaimRecord.ClaimStatus.SUCCESS;
        } else if ("RATE_LIMITED".equals(result.errorMessage())) {
            status = ClaimRecord.ClaimStatus.RATE_LIMITED;
        } else {
            status = ClaimRecord.ClaimStatus.FAILED;
        }

        return ClaimRecord.builder()
            .faucetName(faucet.getName())
            .network(faucet.getNetwork())
            .walletAddress(wallet)
            .status(status)
            .ethAmount(result.ethAmount())
            .txHash(result.txHash())
            .errorMessage(result.errorMessage())
            .powSolveDurationMs(result.powSolveDurationMs())
            .claimedAt(LocalDateTime.now())
            .build();
    }

    private ClaimRecord buildRecord(FaucetClient faucet, ClaimRecord.ClaimStatus status, String error) {
        return ClaimRecord.builder()
            .faucetName(faucet.getName())
            .network(faucet.getNetwork())
            .walletAddress(appConfig.getWalletAddress())
            .status(status)
            .errorMessage(error)
            .claimedAt(LocalDateTime.now())
            .build();
    }
}
