package com.digitalasset.blockchainservice.controller;

import com.digitalasset.blockchainservice.dto.BlockchainResponse;
import com.digitalasset.blockchainservice.dto.TransferRequest;
import com.digitalasset.blockchainservice.service.BlockchainService;
import com.digitalasset.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.BigInteger;

@RestController
@RequestMapping("/blockchain")
@RequiredArgsConstructor
public class BlockchainController {

    private final BlockchainService blockchainService;

    @GetMapping("/block-number")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BigInteger>> getBlockNumber() {
        return ResponseEntity.ok(ApiResponse.success(blockchainService.getLatestBlockNumber()));
    }

    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(
            @RequestParam String contractAddress,
            @RequestParam String walletAddress) {
        BigDecimal balance = blockchainService.getTokenBalance(contractAddress, walletAddress);
        return ResponseEntity.ok(ApiResponse.success(balance));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlockchainResponse>> transfer(
            @Valid @RequestBody TransferRequest request) {
        BlockchainResponse response = blockchainService.transferTokens(
                request.getContractAddress(),
                request.getFromAddress(),
                request.getToAddress(),
                request.getAmount()
        );
        return ResponseEntity.ok(ApiResponse.success("Transfer submitted", response));
    }

    @GetMapping("/transaction/{txHash}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BlockchainResponse>> getTransaction(@PathVariable String txHash) {
        return ResponseEntity.ok(ApiResponse.success(blockchainService.getTransactionReceipt(txHash)));
    }
}
