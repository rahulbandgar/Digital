package com.digitalasset.common.events;

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
public class SettlementCompletedEvent {

    public static final String TOPIC = "settlement-completed";

    private String eventId;
    private String settlementId;
    private Long buyerUserId;
    private Long sellerUserId;
    private BigDecimal tokenAmount;
    private BigDecimal settlementAmount;
    private String currency;
    private String blockchainTxHash;
    private String status;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
