package com.digitalasset.common.dto;

import com.digitalasset.common.enums.AssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDto {
    private Long id;
    private Long userId;
    private String walletAddress;
    private AssetType assetType;
    private BigDecimal balance;
    private Instant createdAt;
}
