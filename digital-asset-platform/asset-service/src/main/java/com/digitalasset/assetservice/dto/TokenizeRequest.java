package com.digitalasset.assetservice.dto;

import com.digitalasset.common.enums.AssetType;
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
public class TokenizeRequest {

    @NotBlank(message = "Asset name is required")
    private String name;

    private String description;

    @NotNull(message = "Asset type is required")
    private AssetType assetType;

    @NotNull(message = "Physical quantity is required")
    @DecimalMin(value = "0.000001", message = "Physical quantity must be positive")
    private BigDecimal physicalQuantity;

    @NotNull(message = "Token ratio is required")
    @DecimalMin(value = "0.000001", message = "Token ratio must be positive")
    private BigDecimal tokenRatio;

    private String vaultRef;
}
