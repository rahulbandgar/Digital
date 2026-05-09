package com.digitalasset.userservice.dto;

import com.digitalasset.common.enums.KycStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycUpdateRequest {

    @NotNull(message = "KYC status is required")
    private KycStatus kycStatus;

    private String notes;
    private String documentRef;
}
