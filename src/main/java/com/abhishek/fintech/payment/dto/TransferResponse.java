package com.abhishek.fintech.payment.dto;

import com.abhishek.fintech.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private UUID paymentId;
    private String referenceId;
    private UUID fromWalletId;
    private UUID toWalletId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private UUID ledgerTransactionId;
    private String description;
    private Instant timestamp;
}
