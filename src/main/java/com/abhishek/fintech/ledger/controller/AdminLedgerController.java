package com.abhishek.fintech.ledger.controller;

import com.abhishek.fintech.ledger.dto.LedgerAuditResponse;
import com.abhishek.fintech.ledger.dto.LedgerEntryResponse;
import com.abhishek.fintech.ledger.dto.LedgerTransactionResponse;
import com.abhishek.fintech.ledger.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/ledger")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Ledger", description = "Administrative endpoints for ledger audits, reconciliation, and inspection")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminLedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/transactions")
    @Operation(summary = "Get all ledger transactions system-wide (Admin only)")
    public ResponseEntity<Page<LedgerTransactionResponse>> getAllTransactions(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ledgerService.getAllTransactions(pageable));
    }

    @GetMapping("/wallets/{walletId}/entries")
    @Operation(summary = "Get all ledger entries for any wallet (Admin only)")
    public ResponseEntity<Page<LedgerEntryResponse>> getWalletEntriesAdmin(
            @PathVariable UUID walletId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ledgerService.getWalletLedgerEntries(walletId, pageable));
    }

    @GetMapping("/wallets/{walletId}/audit")
    @Operation(summary = "Audit wallet balance against double-entry ledger records to verify consistency (Admin only)")
    public ResponseEntity<LedgerAuditResponse> auditWalletBalance(
            @PathVariable UUID walletId
    ) {
        return ResponseEntity.ok(ledgerService.auditWalletBalance(walletId));
    }
}
