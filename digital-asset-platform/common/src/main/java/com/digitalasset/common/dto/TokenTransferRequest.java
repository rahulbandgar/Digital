package com.digitalasset.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class TokenTransferRequest {

    @NotBlank(message = "Source wallet address is required")
    private String fromWalletAddress;

    @NotBlank(message = "Destination wallet address is required")
    private String toWalletAddress;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.000001", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String memo;
}
