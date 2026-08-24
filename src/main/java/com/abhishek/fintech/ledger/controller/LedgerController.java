package com.abhishek.fintech.ledger.controller;

import com.abhishek.fintech.ledger.dto.LedgerEntryResponse;
import com.abhishek.fintech.ledger.dto.LedgerTransactionResponse;
import com.abhishek.fintech.ledger.service.LedgerService;
import com.abhishek.fintech.security.SecurityUser;
import com.abhishek.fintech.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
@Tag(name = "Ledger", description = "Endpoints for querying double-entry ledger transactions and statements")
@SecurityRequirement(name = "Bearer Authentication")
public class LedgerController {

    private final LedgerService ledgerService;
    private final WalletService walletService;

    @GetMapping("/transactions/{referenceId}")
    @Operation(summary = "Get ledger transaction details by reference ID")
    public ResponseEntity<LedgerTransactionResponse> getTransactionByReference(
            @PathVariable String referenceId
    ) {
        return ResponseEntity.ok(ledgerService.getTransactionByReferenceId(referenceId));
    }

    @GetMapping("/wallets/{walletId}/entries")
    @Operation(summary = "Get paginated double-entry line items (statement) for a specific wallet")
    public ResponseEntity<Page<LedgerEntryResponse>> getWalletEntries(
            @PathVariable UUID walletId,
            @AuthenticationPrincipal SecurityUser currentUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        // Enforce ownership: ensure the wallet belongs to the authenticated user
        walletService.getWalletById(walletId, currentUser.getId());
        return ResponseEntity.ok(ledgerService.getWalletLedgerEntries(walletId, pageable));
    }
}
