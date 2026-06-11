package com.digital.ethaccumulator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "claim_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String faucetName;

    @Column(nullable = false)
    private String network; // sepolia | holesky

    @Column(nullable = false)
    private String walletAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    private BigDecimal ethAmount; // ETH received (null if failed)

    private String txHash; // transaction hash from faucet

    @Column(length = 2000)
    private String errorMessage;

    private Long powSolveDurationMs; // how long PoW took to solve

    @Column(nullable = false)
    private LocalDateTime claimedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (claimedAt == null) {
            claimedAt = LocalDateTime.now();
        }
    }

    public enum ClaimStatus {
        SUCCESS, FAILED, RATE_LIMITED, SKIPPED
    }
}
