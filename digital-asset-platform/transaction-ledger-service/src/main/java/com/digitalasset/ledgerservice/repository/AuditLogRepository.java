package com.digitalasset.ledgerservice.repository;

import com.digitalasset.common.enums.TransactionType;
import com.digitalasset.ledgerservice.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByFromUserIdOrToUserId(Long fromUserId, Long toUserId, Pageable pageable);

    Page<AuditLog> findByTransactionType(TransactionType type, Pageable pageable);

    List<AuditLog> findByBlockchainTxHash(String txHash);

    Page<AuditLog> findByRecordedAtBetween(Instant from, Instant to, Pageable pageable);

    boolean existsByEventId(String eventId);
}
