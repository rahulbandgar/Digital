package com.digitalasset.ledgerservice.service;

import com.digitalasset.common.enums.TransactionType;
import com.digitalasset.ledgerservice.entity.AuditLog;
import com.digitalasset.ledgerservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog save(AuditLog entry) {
        if (auditLogRepository.existsByEventId(entry.getEventId())) {
            log.debug("Duplicate event ignored: {}", entry.getEventId());
            return entry;
        }
        return auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getUserHistory(Long userId, Pageable pageable) {
        return auditLogRepository.findByFromUserIdOrToUserId(userId, userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getByType(TransactionType type, Pageable pageable) {
        return auditLogRepository.findByTransactionType(type, pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getByTxHash(String txHash) {
        return auditLogRepository.findByBlockchainTxHash(txHash);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getByDateRange(Instant from, Instant to, Pageable pageable) {
        return auditLogRepository.findByRecordedAtBetween(from, to, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
