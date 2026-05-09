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
public class TokenTransferredEvent {

    public static final String TOPIC = "token-transferred";

    private String eventId;
    private AssetType assetType;
    private BigDecimal amount;
    private String fromWalletAddress;
    private String toWalletAddress;
    private Long fromUserId;
    private Long toUserId;
    private String blockchainTxHash;
    private String memo;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
