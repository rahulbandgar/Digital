package com.digitalasset.assetservice.controller;

import com.digitalasset.assetservice.dto.BurnRequest;
import com.digitalasset.assetservice.dto.TokenizeRequest;
import com.digitalasset.assetservice.entity.Asset;
import com.digitalasset.assetservice.entity.TokenOperation;
import com.digitalasset.assetservice.service.AssetService;
import com.digitalasset.common.dto.ApiResponse;
import com.digitalasset.common.dto.MintRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping("/assets/tokenize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Asset>> tokenize(
            @Valid @RequestBody TokenizeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Asset asset = assetService.tokenize(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Asset tokenized successfully", asset));
    }

    @GetMapping("/assets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Asset>>> getAllAssets() {
        return ResponseEntity.ok(ApiResponse.success(assetService.getAllActiveAssets()));
    }

    @GetMapping("/assets/{assetId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Asset>> getAsset(@PathVariable Long assetId) {
        return ResponseEntity.ok(ApiResponse.success(assetService.getAsset(assetId)));
    }

    @PostMapping("/tokens/mint")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TokenOperation>> mint(
            @Valid @RequestBody MintRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String blockchainTxHash) {
        TokenOperation op = assetService.mint(request, userDetails.getUsername(), blockchainTxHash);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tokens minted successfully", op));
    }

    @PostMapping("/tokens/burn")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TokenOperation>> burn(
            @Valid @RequestBody BurnRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String blockchainTxHash) {
        TokenOperation op = assetService.burn(request, userDetails.getUsername(), blockchainTxHash);
        return ResponseEntity.ok(ApiResponse.success("Tokens burned successfully", op));
    }

    @GetMapping("/assets/{assetId}/operations")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<ApiResponse<List<TokenOperation>>> getOperations(@PathVariable Long assetId) {
        return ResponseEntity.ok(ApiResponse.success(assetService.getOperationsByAsset(assetId)));
    }
}
