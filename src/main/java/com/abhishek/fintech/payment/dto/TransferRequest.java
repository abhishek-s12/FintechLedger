package com.abhishek.fintech.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class TransferRequest {

    @NotNull(message = "Sender wallet ID is required")
    private UUID fromWalletId;

    @NotNull(message = "Receiver wallet ID is required")
    private UUID toWalletId;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be at least 0.01")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
}
