package com.digitalasset.common.events;

import com.digitalasset.common.enums.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycVerifiedEvent {

    public static final String TOPIC = "kyc-verified";

    private String eventId;
    private Long userId;
    private String username;
    private KycStatus kycStatus;
    private String verifiedBy;
    private String notes;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
