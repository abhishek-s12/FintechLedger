package com.abhishek.fintech.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletCreatedEvent {
    private String eventId;
    private String eventType;
    private UUID walletId;
    private UUID userId;
    private String currency;
    private Instant timestamp;
}
