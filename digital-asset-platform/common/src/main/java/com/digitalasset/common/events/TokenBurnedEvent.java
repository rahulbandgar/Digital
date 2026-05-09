package com.digitalasset.common.events;

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
public class TokenBurnedEvent {

    public static final String TOPIC = "token-burned";

    private String eventId;
    private AssetType assetType;
    private BigDecimal amount;
    private Long walletId;
    private String walletAddress;
    private String blockchainTxHash;
    private String burnedBy;
    private String reason;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
