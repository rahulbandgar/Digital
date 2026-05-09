package com.digitalasset.walletservice.controller;

import com.digitalasset.common.dto.ApiResponse;
import com.digitalasset.common.dto.TokenTransferRequest;
import com.digitalasset.common.dto.WalletDto;
import com.digitalasset.common.enums.AssetType;
import com.digitalasset.walletservice.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WalletDto>> createWallet(
            @RequestParam Long userId,
            @RequestParam AssetType assetType) {
        WalletDto wallet = walletService.createWallet(userId, assetType);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wallet created successfully", wallet));
    }

    @GetMapping("/{walletId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WalletDto>> getWallet(@PathVariable Long walletId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWallet(walletId)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<WalletDto>>> getUserWallets(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getUserWallets(userId)));
    }

    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WalletDto>> getBalance(@RequestParam String walletAddress) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getBalance(walletAddress)));
    }

    @PostMapping("/transfer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> transfer(@Valid @RequestBody TokenTransferRequest request) {
        walletService.transfer(
                request.getFromWalletAddress(),
                request.getToWalletAddress(),
                request.getAmount(),
                null, null,
                request.getMemo(),
                null
        );
        return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully", null));
    }
}
