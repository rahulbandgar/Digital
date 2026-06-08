package com.digital.ethaccumulator;

import com.digital.ethaccumulator.model.ClaimRecord;
import com.digital.ethaccumulator.service.AccumulatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Active only under the "batch" profile.
 * Claims from all faucets once, prints a summary, then exits.
 * Used by GitHub Actions: SPRING_PROFILES_ACTIVE=batch
 */
@Slf4j
@Component
@Profile("batch")
@RequiredArgsConstructor
public class ClaimRunner implements ApplicationRunner {

    private final AccumulatorService accumulatorService;
    private final ApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Batch claim run starting ===");
        List<ClaimRecord> results = accumulatorService.runDailyClaims();

        System.out.println("\n========= CLAIM RESULTS =========");
        for (ClaimRecord r : results) {
            System.out.printf("%-25s | %-12s | ETH: %-20s | TX: %s%n",
                r.getFaucetName(), r.getStatus(),
                r.getEthAmount() != null ? r.getEthAmount().toPlainString() : "—",
                r.getTxHash() != null ? r.getTxHash() : "—");
        }
        long successes = results.stream().filter(r -> r.getStatus() == ClaimRecord.ClaimStatus.SUCCESS).count();
        System.out.printf("==================================%n");
        System.out.printf("Total: %d/%d faucets succeeded%n%n", successes, results.size());

        SpringApplication.exit(context, () -> successes > 0 ? 0 : 1);
    }
}
