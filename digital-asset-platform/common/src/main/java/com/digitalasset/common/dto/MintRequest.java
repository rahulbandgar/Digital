package com.digitalasset.common.dto;

import com.digitalasset.common.enums.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MintRequest {

    @NotNull(message = "Asset type is required")
    private AssetType assetType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.000001", message = "Mint amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Recipient wallet ID is required")
    private Long recipientWalletId;

    private String reason;
}
