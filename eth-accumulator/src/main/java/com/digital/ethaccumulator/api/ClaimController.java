package com.digital.ethaccumulator.api;

import com.digital.ethaccumulator.model.ClaimRecord;
import com.digital.ethaccumulator.repository.ClaimRepository;
import com.digital.ethaccumulator.service.AccumulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimRepository claimRepository;
    private final AccumulatorService accumulatorService;

    @GetMapping
    public List<ClaimRecord> recentClaims() {
        return claimRepository.findTop50ByOrderByClaimedAtDesc();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        BigDecimal totalEth = claimRepository.sumSuccessfulEth();
        List<ClaimRecord> recent = claimRepository.findTop50ByOrderByClaimedAtDesc();
        long successCount = recent.stream()
            .filter(r -> r.getStatus() == ClaimRecord.ClaimStatus.SUCCESS)
            .count();

        return Map.of(
            "totalEthAccumulated", totalEth,
            "successfulClaims", claimRepository.findByClaimedAtBetweenOrderByClaimedAtDesc(
                LocalDateTime.now().minusDays(30), LocalDateTime.now()).stream()
                .filter(r -> r.getStatus() == ClaimRecord.ClaimStatus.SUCCESS).count(),
            "recentSuccessCount", successCount,
            "asOf", LocalDateTime.now()
        );
    }

    // Manually trigger a claim run (useful for testing)
    @PostMapping("/trigger")
    public ResponseEntity<List<ClaimRecord>> triggerManual() {
        List<ClaimRecord> results = accumulatorService.runDailyClaims();
        return ResponseEntity.ok(results);
    }
}
