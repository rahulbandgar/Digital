package com.digitalasset.assetservice.entity;

import com.digitalasset.common.enums.AssetType;
import com.digitalasset.common.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "token_operations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private TransactionType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal amount;

    @Column(name = "wallet_id")
    private Long walletId;

    @Column(name = "wallet_address", length = 100)
    private String walletAddress;

    @Column(name = "blockchain_tx_hash", length = 100)
    private String blockchainTxHash;

    @Column(name = "performed_by", nullable = false, length = 50)
    private String performedBy;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
