package com.digitalasset.settlementservice.repository;

import com.digitalasset.settlementservice.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findBySettlementId(String settlementId);
    Page<Settlement> findByBuyerUserIdOrSellerUserId(Long buyerId, Long sellerId, Pageable pageable);
    Page<Settlement> findByStatus(Settlement.SettlementStatus status, Pageable pageable);
}
