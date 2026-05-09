package com.digitalasset.assetservice.entity;

import com.digitalasset.common.enums.AssetType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "assets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    /** Total physical quantity backing this asset (e.g. grams of gold) */
    @Column(name = "physical_quantity", nullable = false, precision = 30, scale = 8)
    private BigDecimal physicalQuantity;

    /** Total tokens minted for this asset */
    @Column(name = "total_supply", nullable = false, precision = 30, scale = 8)
    @Builder.Default
    private BigDecimal totalSupply = BigDecimal.ZERO;

    /** Token per unit of physical asset (e.g. 1 token = 1 gram) */
    @Column(name = "token_ratio", nullable = false, precision = 30, scale = 8)
    @Builder.Default
    private BigDecimal tokenRatio = BigDecimal.ONE;

    @Column(name = "smart_contract_address", length = 100)
    private String smartContractAddress;

    @Column(name = "vault_ref", length = 255)
    private String vaultRef;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
