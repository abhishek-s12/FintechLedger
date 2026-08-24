package com.abhishek.fintech.payment.controller;

import com.abhishek.fintech.payment.dto.PaymentResponse;
import com.abhishek.fintech.payment.dto.TransferRequest;
import com.abhishek.fintech.payment.dto.TransferResponse;
import com.abhishek.fintech.payment.service.PaymentService;
import com.abhishek.fintech.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Endpoints for money transfers, payment processing, and payment histories")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds between two wallets with strict double-entry ledger guarantee and idempotency")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @Parameter(description = "Unique client-generated idempotency key (UUID/hash) to prevent duplicate execution")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        TransferResponse response = paymentService.transfer(currentUser.getId(), request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{referenceId}")
    @Operation(summary = "Get payment details by reference ID")
    public ResponseEntity<PaymentResponse> getPaymentByReference(
            @PathVariable String referenceId,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        return ResponseEntity.ok(paymentService.getPaymentByReferenceId(referenceId, currentUser.getId()));
    }

    @GetMapping("/wallet/{walletId}")
    @Operation(summary = "Get payment history for a specific wallet")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsForWallet(
            @PathVariable UUID walletId,
            @AuthenticationPrincipal SecurityUser currentUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(paymentService.getPaymentsForWallet(walletId, currentUser.getId(), pageable));
    }

    @GetMapping("/history")
    @Operation(summary = "Get all payments for the authenticated user")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsForUser(
            @AuthenticationPrincipal SecurityUser currentUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(paymentService.getPaymentsForUser(currentUser.getId(), pageable));
    }
}
