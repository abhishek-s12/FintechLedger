package com.abhishek.fintech.ledger.dto;

import com.abhishek.fintech.ledger.entity.TransactionStatus;
import com.abhishek.fintech.ledger.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerTransactionResponse {
    private UUID id;
    private String referenceId;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private Instant createdAt;
    private List<LedgerEntryResponse> entries;
}
