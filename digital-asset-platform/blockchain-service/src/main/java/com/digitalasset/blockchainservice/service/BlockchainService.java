package com.digitalasset.blockchainservice.service;

import com.digitalasset.blockchainservice.dto.BlockchainResponse;
import com.digitalasset.common.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainService {

    private final Web3j web3j;
    private final ContractGasProvider gasProvider;

    @Value("${blockchain.admin-private-key:0x0000000000000000000000000000000000000000000000000000000000000001}")
    private String adminPrivateKey;

    @Value("${blockchain.chain-id:1337}")
    private long chainId;

    public BigInteger getLatestBlockNumber() {
        try {
            EthBlockNumber result = web3j.ethBlockNumber().send();
            log.debug("Latest block number: {}", result.getBlockNumber());
            return result.getBlockNumber();
        } catch (IOException e) {
            log.error("Failed to get block number", e);
            throw BusinessException.badRequest("Blockchain node unreachable: " + e.getMessage());
        }
    }

    public BigDecimal getTokenBalance(String contractAddress, String walletAddress) {
        try {
            org.web3j.protocol.core.methods.request.Transaction call =
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                            walletAddress,
                            contractAddress,
                            encodeBalanceOf(walletAddress)
                    );
            EthCall response = web3j.ethCall(call, DefaultBlockParameterName.LATEST).send();
            if (response.hasError()) {
                throw BusinessException.badRequest("Contract call error: " + response.getError().getMessage());
            }
            String hexResult = response.getValue();
            if (hexResult == null || hexResult.equals("0x")) return BigDecimal.ZERO;
            BigInteger raw = new BigInteger(hexResult.substring(2), 16);
            return new BigDecimal(raw).movePointLeft(18);
        } catch (IOException e) {
            log.error("Failed to get token balance", e);
            throw BusinessException.badRequest("Balance query failed: " + e.getMessage());
        }
    }

    public BlockchainResponse transferTokens(String contractAddress,
                                             String fromAddress,
                                             String toAddress,
                                             BigDecimal amount) {
        try {
            Credentials credentials = Credentials.create(adminPrivateKey);
            RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, chainId);

            BigInteger amountWei = amount.movePointRight(18).toBigInteger();
            String data = encodeTransfer(toAddress, amountWei);

            EthSendTransaction response = txManager.sendTransaction(
                    gasProvider.getGasPrice(null),
                    gasProvider.getGasLimit(null),
                    contractAddress,
                    data,
                    BigInteger.ZERO
            );

            if (response.hasError()) {
                throw BusinessException.badRequest("Transaction error: " + response.getError().getMessage());
            }

            String txHash = response.getTransactionHash();
            log.info("Token transfer submitted: {} → {} amount={} tx={}", fromAddress, toAddress, amount, txHash);

            return BlockchainResponse.builder()
                    .transactionHash(txHash)
                    .status("PENDING")
                    .details("Transfer of " + amount + " tokens submitted")
                    .build();

        } catch (IOException e) {
            log.error("Transfer failed", e);
            throw BusinessException.badRequest("Transfer failed: " + e.getMessage());
        }
    }

    public BlockchainResponse getTransactionReceipt(String txHash) {
        try {
            EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash).send();
            return receipt.getTransactionReceipt()
                    .map(r -> BlockchainResponse.builder()
                            .transactionHash(r.getTransactionHash())
                            .blockNumber(r.getBlockNumber())
                            .status("0x1".equals(r.getStatus()) ? "SUCCESS" : "FAILED")
                            .build())
                    .orElse(BlockchainResponse.builder()
                            .transactionHash(txHash)
                            .status("PENDING")
                            .build());
        } catch (IOException e) {
            log.error("Failed to get receipt for {}", txHash, e);
            throw BusinessException.badRequest("Receipt query failed: " + e.getMessage());
        }
    }

    /** ERC-20 balanceOf(address) selector + padded address */
    private String encodeBalanceOf(String address) {
        String paddedAddr = address.replace("0x", "").toLowerCase();
        while (paddedAddr.length() < 64) paddedAddr = "0" + paddedAddr;
        return "0x70a08231" + paddedAddr;
    }

    /** ERC-20 transfer(address,uint256) selector */
    private String encodeTransfer(String to, BigInteger amount) {
        String paddedTo = to.replace("0x", "").toLowerCase();
        while (paddedTo.length() < 64) paddedTo = "0" + paddedTo;
        String paddedAmt = amount.toString(16);
        while (paddedAmt.length() < 64) paddedAmt = "0" + paddedAmt;
        return "0xa9059cbb" + paddedTo + paddedAmt;
    }
}
