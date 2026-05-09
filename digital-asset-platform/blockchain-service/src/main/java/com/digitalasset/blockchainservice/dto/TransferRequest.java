package com.digitalasset.blockchainservice.dto;

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
public class TransferRequest {

    @NotBlank(message = "Contract address is required")
    private String contractAddress;

    @NotBlank(message = "From address is required")
    private String fromAddress;

    @NotBlank(message = "To address is required")
    private String toAddress;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.000001", message = "Amount must be positive")
    private BigDecimal amount;
}
