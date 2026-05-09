package com.digitalasset.settlementservice.controller;

import com.digitalasset.common.dto.ApiResponse;
import com.digitalasset.settlementservice.dto.SettlementRequest;
import com.digitalasset.settlementservice.entity.Settlement;
import com.digitalasset.settlementservice.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Settlement>> initiate(
            @Valid @RequestBody SettlementRequest request) {
        Settlement settlement = settlementService.initiateSettlement(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Settlement initiated", settlement));
    }

    @PostMapping("/{settlementId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Settlement>> complete(
            @PathVariable String settlementId,
            @RequestParam String blockchainTxHash) {
        Settlement settlement = settlementService.completeSettlement(settlementId, blockchainTxHash);
        return ResponseEntity.ok(ApiResponse.success("Settlement completed", settlement));
    }

    @PostMapping("/{settlementId}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Settlement>> fail(
            @PathVariable String settlementId,
            @RequestParam String reason) {
        Settlement settlement = settlementService.failSettlement(settlementId, reason);
        return ResponseEntity.ok(ApiResponse.success("Settlement marked as failed", settlement));
    }

    @GetMapping("/{settlementId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Settlement>> get(@PathVariable String settlementId) {
        return ResponseEntity.ok(ApiResponse.success(settlementService.getSettlement(settlementId)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ApiResponse<Page<Settlement>>> getUserSettlements(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Settlement> settlements = settlementService.getUserSettlements(userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(settlements));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<ApiResponse<Page<Settlement>>> getByStatus(
            @PathVariable Settlement.SettlementStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Settlement> settlements = settlementService.getByStatus(status,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(settlements));
    }
}
