package com.digitalasset.settlementservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementRequest {

    @NotNull(message = "Buyer user ID is required")
    private Long buyerUserId;

    @NotNull(message = "Seller user ID is required")
    private Long sellerUserId;

    @NotBlank(message = "Buyer wallet address is required")
    private String buyerWalletAddress;

    @NotBlank(message = "Seller wallet address is required")
    private String sellerWalletAddress;

    @NotNull(message = "Token amount is required")
    @DecimalMin(value = "0.000001", message = "Token amount must be positive")
    private BigDecimal tokenAmount;

    @NotNull(message = "Settlement amount is required")
    @DecimalMin(value = "0.01", message = "Settlement amount must be positive")
    private BigDecimal settlementAmount;

    @NotBlank(message = "Currency is required")
    private String currency;
}
