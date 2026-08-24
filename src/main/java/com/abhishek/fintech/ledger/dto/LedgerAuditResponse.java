package com.abhishek.fintech.ledger.dto;

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
public class LedgerAuditResponse {
    private UUID walletId;
    private String currency;
    private BigDecimal walletBalance;
    private BigDecimal ledgerCalculatedBalance;
    private BigDecimal discrepancy;
    private boolean isConsistent;
    private Instant auditedAt;
}
