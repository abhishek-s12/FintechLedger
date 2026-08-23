package com.abhishek.fintech.wallet.controller;

import com.abhishek.fintech.wallet.dto.WalletResponse;
import com.abhishek.fintech.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/wallets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Wallets", description = "Administrative wallet operations (Freeze, Unfreeze)")
@SecurityRequirement(name = "BearerAuth")
public class AdminWalletController {

    private final WalletService walletService;

    @PatchMapping("/{id}/freeze")
    @Operation(summary = "Freeze a wallet", description = "Admin endpoint to freeze a wallet, preventing any subsequent transfers.")
    public ResponseEntity<WalletResponse> freezeWallet(@PathVariable UUID id) {
        return ResponseEntity.ok(walletService.freezeWallet(id));
    }

    @PatchMapping("/{id}/unfreeze")
    @Operation(summary = "Unfreeze a wallet", description = "Admin endpoint to unfreeze a previously frozen wallet.")
    public ResponseEntity<WalletResponse> unfreezeWallet(@PathVariable UUID id) {
        return ResponseEntity.ok(walletService.unfreezeWallet(id));
    }
}
