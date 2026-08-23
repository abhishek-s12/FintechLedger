package com.abhishek.fintech.wallet.controller;

import com.abhishek.fintech.security.SecurityUser;
import com.abhishek.fintech.wallet.dto.CreateWalletRequest;
import com.abhishek.fintech.wallet.dto.DepositRequest;
import com.abhishek.fintech.wallet.dto.WalletBalanceResponse;
import com.abhishek.fintech.wallet.dto.WalletResponse;
import com.abhishek.fintech.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "Wallet management, balance inquiry, and deposits")
@SecurityRequirement(name = "BearerAuth")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(summary = "Create a new wallet", description = "Creates a new currency wallet for the authenticated user.")
    public ResponseEntity<WalletResponse> createWallet(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Valid @RequestBody CreateWalletRequest request
    ) {
        WalletResponse response = walletService.createWallet(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List user wallets", description = "Retrieves all wallets owned by the authenticated user.")
    public ResponseEntity<List<WalletResponse>> getWallets(@AuthenticationPrincipal SecurityUser currentUser) {
        return ResponseEntity.ok(walletService.getWalletsByUserId(currentUser.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get wallet details", description = "Retrieves details of a specific wallet owned by the user.")
    public ResponseEntity<WalletResponse> getWalletById(
            @AuthenticationPrincipal SecurityUser currentUser,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(walletService.getWalletById(id, currentUser.getId()));
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Get wallet balance", description = "Retrieves the current available balance for a specific wallet.")
    public ResponseEntity<WalletBalanceResponse> getWalletBalance(
            @AuthenticationPrincipal SecurityUser currentUser,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(walletService.getWalletBalance(id, currentUser.getId()));
    }

    @PostMapping("/{id}/deposit")
    @Operation(summary = "Deposit funds into wallet", description = "Credits funds into the user's active wallet account.")
    public ResponseEntity<WalletResponse> deposit(
            @AuthenticationPrincipal SecurityUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody DepositRequest request
    ) {
        return ResponseEntity.ok(walletService.deposit(id, currentUser.getId(), request));
    }
}
