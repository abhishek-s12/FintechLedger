package com.abhishek.fintech.wallet.dto;

import com.abhishek.fintech.wallet.entity.WalletStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Wallet Balance Response")
public class WalletBalanceResponse {

    private UUID walletId;
    private String currency;
    private BigDecimal balance;
    private WalletStatus status;
}
