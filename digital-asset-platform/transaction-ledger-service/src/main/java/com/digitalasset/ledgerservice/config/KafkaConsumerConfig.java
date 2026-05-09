package com.digitalasset.ledgerservice.config;

import com.digitalasset.common.events.*;
import com.digitalasset.common.enums.TransactionType;
import com.digitalasset.ledgerservice.entity.AuditLog;
import com.digitalasset.ledgerservice.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final AuditLogService auditLogService;

    @KafkaListener(topics = TokenMintedEvent.TOPIC, groupId = "ledger-group")
    public void onTokenMinted(TokenMintedEvent event) {
        log.info("Received TOKEN_MINTED event: {}", event.getEventId());
        AuditLog entry = AuditLog.builder()
                .eventId(event.getEventId())
                .transactionType(TransactionType.MINT)
                .assetType(event.getAssetType())
                .toWallet(event.getRecipientWalletAddress())
                .toUserId(event.getRecipientWalletId())
                .amount(event.getAmount())
                .blockchainTxHash(event.getBlockchainTxHash())
                .kafkaTopic(TokenMintedEvent.TOPIC)
                .metadata("mintedBy=" + event.getMintedBy())
                .build();
        auditLogService.save(entry);
    }

    @KafkaListener(topics = TokenTransferredEvent.TOPIC, groupId = "ledger-group")
    public void onTokenTransferred(TokenTransferredEvent event) {
        log.info("Received TOKEN_TRANSFERRED event: {}", event.getEventId());
        AuditLog entry = AuditLog.builder()
                .eventId(event.getEventId())
                .transactionType(TransactionType.TRANSFER)
                .assetType(event.getAssetType())
                .fromWallet(event.getFromWalletAddress())
                .toWallet(event.getToWalletAddress())
                .fromUserId(event.getFromUserId())
                .toUserId(event.getToUserId())
                .amount(event.getAmount())
                .blockchainTxHash(event.getBlockchainTxHash())
                .kafkaTopic(TokenTransferredEvent.TOPIC)
                .metadata("memo=" + event.getMemo())
                .build();
        auditLogService.save(entry);
    }

    @KafkaListener(topics = TokenBurnedEvent.TOPIC, groupId = "ledger-group")
    public void onTokenBurned(TokenBurnedEvent event) {
        log.info("Received TOKEN_BURNED event: {}", event.getEventId());
        AuditLog entry = AuditLog.builder()
                .eventId(event.getEventId())
                .transactionType(TransactionType.BURN)
                .assetType(event.getAssetType())
                .fromWallet(event.getWalletAddress())
                .fromUserId(event.getWalletId())
                .amount(event.getAmount())
                .blockchainTxHash(event.getBlockchainTxHash())
                .kafkaTopic(TokenBurnedEvent.TOPIC)
                .metadata("reason=" + event.getReason())
                .build();
        auditLogService.save(entry);
    }

    @KafkaListener(topics = SettlementCompletedEvent.TOPIC, groupId = "ledger-group")
    public void onSettlementCompleted(SettlementCompletedEvent event) {
        log.info("Received SETTLEMENT_COMPLETED event: {}", event.getEventId());
        AuditLog entry = AuditLog.builder()
                .eventId(event.getEventId())
                .transactionType(TransactionType.SETTLEMENT)
                .fromUserId(event.getSellerUserId())
                .toUserId(event.getBuyerUserId())
                .amount(event.getTokenAmount())
                .blockchainTxHash(event.getBlockchainTxHash())
                .kafkaTopic(SettlementCompletedEvent.TOPIC)
                .metadata("settlementId=" + event.getSettlementId() + ",currency=" + event.getCurrency())
                .build();
        auditLogService.save(entry);
    }
}
