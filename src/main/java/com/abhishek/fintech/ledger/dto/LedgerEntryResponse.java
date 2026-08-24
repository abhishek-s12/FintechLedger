package com.abhishek.fintech.ledger.dto;

import com.abhishek.fintech.ledger.entity.EntryType;
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
public class LedgerEntryResponse {
    private UUID id;
    private UUID transactionId;
    private UUID walletId;
    private EntryType entryType;
    private BigDecimal amount;
    private String currency;
    private Instant createdAt;
}
