package com.digitalasset.settlementservice.service;

import com.digitalasset.common.events.SettlementCompletedEvent;
import com.digitalasset.common.exceptions.BusinessException;
import com.digitalasset.settlementservice.dto.SettlementRequest;
import com.digitalasset.settlementservice.entity.Settlement;
import com.digitalasset.settlementservice.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Initiates a Delivery-vs-Payment (DvP) settlement.
     * In production this would call wallet-service and blockchain-service.
     * The transaction is atomic: tokens move only if payment is confirmed.
     */
    @Transactional
    public Settlement initiateSettlement(SettlementRequest request) {
        if (request.getBuyerUserId().equals(request.getSellerUserId())) {
            throw BusinessException.badRequest("Buyer and seller cannot be the same user");
        }

        Settlement settlement = Settlement.builder()
                .settlementId(UUID.randomUUID().toString())
                .buyerUserId(request.getBuyerUserId())
                .sellerUserId(request.getSellerUserId())
                .buyerWalletAddress(request.getBuyerWalletAddress())
                .sellerWalletAddress(request.getSellerWalletAddress())
                .tokenAmount(request.getTokenAmount())
                .settlementAmount(request.getSettlementAmount())
                .currency(request.getCurrency())
                .status(Settlement.SettlementStatus.INITIATED)
                .build();

        Settlement saved = settlementRepository.save(settlement);
        log.info("Settlement initiated: {} buyer={} seller={} tokens={}",
                saved.getSettlementId(), request.getBuyerUserId(),
                request.getSellerUserId(), request.getTokenAmount());
        return saved;
    }

    /**
     * Completes settlement after blockchain confirmation.
     * Publishes SETTLEMENT_COMPLETED Kafka event consumed by the ledger service.
     */
    @Transactional
    public Settlement completeSettlement(String settlementId, String blockchainTxHash) {
        Settlement settlement = settlementRepository.findBySettlementId(settlementId)
                .orElseThrow(() -> BusinessException.notFound("Settlement", settlementId));

        if (settlement.getStatus() != Settlement.SettlementStatus.INITIATED
                && settlement.getStatus() != Settlement.SettlementStatus.PENDING_BLOCKCHAIN) {
            throw BusinessException.badRequest("Settlement cannot be completed in status: " + settlement.getStatus());
        }

        settlement.setStatus(Settlement.SettlementStatus.COMPLETED);
        settlement.setBlockchainTxHash(blockchainTxHash);
        settlement.setSettledAt(Instant.now());
        Settlement saved = settlementRepository.save(settlement);

        SettlementCompletedEvent event = SettlementCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .settlementId(settlementId)
                .buyerUserId(settlement.getBuyerUserId())
                .sellerUserId(settlement.getSellerUserId())
                .tokenAmount(settlement.getTokenAmount())
                .settlementAmount(settlement.getSettlementAmount())
                .currency(settlement.getCurrency())
                .blockchainTxHash(blockchainTxHash)
                .status("COMPLETED")
                .build();

        kafkaTemplate.send(SettlementCompletedEvent.TOPIC, event.getEventId(), event);
        log.info("Settlement completed: {} tx={}", settlementId, blockchainTxHash);
        return saved;
    }

    @Transactional
    public Settlement failSettlement(String settlementId, String reason) {
        Settlement settlement = settlementRepository.findBySettlementId(settlementId)
                .orElseThrow(() -> BusinessException.notFound("Settlement", settlementId));
        settlement.setStatus(Settlement.SettlementStatus.FAILED);
        settlement.setFailureReason(reason);
        log.warn("Settlement failed: {} reason={}", settlementId, reason);
        return settlementRepository.save(settlement);
    }

    @Transactional(readOnly = true)
    public Settlement getSettlement(String settlementId) {
        return settlementRepository.findBySettlementId(settlementId)
                .orElseThrow(() -> BusinessException.notFound("Settlement", settlementId));
    }

    @Transactional(readOnly = true)
    public Page<Settlement> getUserSettlements(Long userId, Pageable pageable) {
        return settlementRepository.findByBuyerUserIdOrSellerUserId(userId, userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Settlement> getByStatus(Settlement.SettlementStatus status, Pageable pageable) {
        return settlementRepository.findByStatus(status, pageable);
    }
}
