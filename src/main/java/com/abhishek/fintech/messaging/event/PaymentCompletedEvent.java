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
public class PaymentCompletedEvent {
    private String eventId;
    private String eventType;
    private UUID paymentId;
    private String referenceId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private UUID senderUserId;
    private UUID receiverUserId;
    private BigDecimal amount;
    private String currency;
    private Instant timestamp;
}
