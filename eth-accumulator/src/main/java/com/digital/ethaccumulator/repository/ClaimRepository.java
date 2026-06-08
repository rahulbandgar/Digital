package com.digital.ethaccumulator.repository;

import com.digital.ethaccumulator.model.ClaimRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<ClaimRecord, Long> {

    List<ClaimRecord> findTop50ByOrderByClaimedAtDesc();

    List<ClaimRecord> findByClaimedAtBetweenOrderByClaimedAtDesc(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(c.ethAmount), 0) FROM ClaimRecord c WHERE c.status = 'SUCCESS'")
    BigDecimal sumSuccessfulEth();

    @Query("SELECT COUNT(c) FROM ClaimRecord c WHERE c.faucetName = :faucetName AND c.status = 'SUCCESS' AND c.claimedAt > :since")
    long countSuccessfulClaimsSince(String faucetName, LocalDateTime since);

    boolean existsByFaucetNameAndClaimedAtAfter(String faucetName, LocalDateTime since);
}
