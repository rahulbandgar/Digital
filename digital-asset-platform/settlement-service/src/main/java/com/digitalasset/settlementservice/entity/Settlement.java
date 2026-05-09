package com.digitalasset.settlementservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "settlements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false, unique = true, length = 50)
    private String settlementId;

    @Column(name = "buyer_user_id", nullable = false)
    private Long buyerUserId;

    @Column(name = "seller_user_id", nullable = false)
    private Long sellerUserId;

    @Column(name = "buyer_wallet_address", nullable = false, length = 100)
    private String buyerWalletAddress;

    @Column(name = "seller_wallet_address", nullable = false, length = 100)
    private String sellerWalletAddress;

    @Column(name = "token_amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal tokenAmount;

    @Column(name = "settlement_amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal settlementAmount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.INITIATED;

    @Column(name = "blockchain_tx_hash", length = 100)
    private String blockchainTxHash;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    public enum SettlementStatus {
        INITIATED, PENDING_BLOCKCHAIN, COMPLETED, FAILED, REVERSED
    }
}
