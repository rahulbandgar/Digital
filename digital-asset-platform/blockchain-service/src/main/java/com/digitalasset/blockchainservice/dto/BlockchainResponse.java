package com.digitalasset.blockchainservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainResponse {
    private String transactionHash;
    private BigInteger blockNumber;
    private String status;
    private String contractAddress;
    private String details;
}
