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
public class PaymentResponse {
    private UUID id;
    private String referenceId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String idempotencyKey;
    private String errorMessage;
    private Instant createdAt;
    private Instant completedAt;
}
