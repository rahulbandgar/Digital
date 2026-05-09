package com.digitalasset.assetservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BurnRequest {

    @NotNull(message = "Asset ID is required")
    private Long assetId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.000001", message = "Burn amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Wallet ID is required")
    private Long walletId;

    private String walletAddress;
    private String reason;
}
