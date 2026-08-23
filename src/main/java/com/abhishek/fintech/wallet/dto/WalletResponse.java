package com.abhishek.fintech.wallet.dto;

import com.abhishek.fintech.wallet.entity.WalletStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Wallet Account Details")
public class WalletResponse {

    private UUID id;
    private UUID userId;
    private String currency;
    private BigDecimal balance;
    private WalletStatus status;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}
