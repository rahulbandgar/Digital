package com.digitalasset.ledgerservice.controller;

import com.digitalasset.common.dto.ApiResponse;
import com.digitalasset.common.enums.TransactionType;
import com.digitalasset.ledgerservice.entity.AuditLog;
import com.digitalasset.ledgerservice.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/ledger")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditLogService.getAll(
                PageRequest.of(page, size, Sort.by("recordedAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR') or #userId == authentication.principal.id")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getUserHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditLogService.getUserHistory(userId,
                PageRequest.of(page, size, Sort.by("recordedAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getByType(
            @PathVariable TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditLogService.getByType(type,
                PageRequest.of(page, size, Sort.by("recordedAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/tx/{txHash}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getByTxHash(@PathVariable String txHash) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getByTxHash(txHash)));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditLogService.getByDateRange(from, to,
                PageRequest.of(page, size, Sort.by("recordedAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
