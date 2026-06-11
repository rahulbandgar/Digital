package com.digital.ethaccumulator.scheduler;

import com.digital.ethaccumulator.service.AccumulatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyClaimScheduler {

    private final AccumulatorService accumulatorService;

    // Default: daily at 08:00 UTC — override with DCA_CRON env var
    @Scheduled(cron = "${accumulator.claim-cron:0 0 8 * * ?}", zone = "UTC")
    public void runDailyClaim() {
        log.info("Scheduled daily claim triggered");
        accumulatorService.runDailyClaims();
    }
}
