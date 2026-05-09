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
public class TokenMintedEvent {

    public static final String TOPIC = "token-minted";

    private String eventId;
    private AssetType assetType;
    private BigDecimal amount;
    private Long recipientWalletId;
    private String recipientWalletAddress;
    private String blockchainTxHash;
    private String mintedBy;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
