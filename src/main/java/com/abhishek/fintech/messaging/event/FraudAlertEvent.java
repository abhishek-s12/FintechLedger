package com.abhishek.fintech.messaging.event;

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
public class FraudAlertEvent {
    private String alertId;
    private String ruleTriggered;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private UUID paymentId;
    private String paymentReferenceId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private Instant detectedAt;
}
