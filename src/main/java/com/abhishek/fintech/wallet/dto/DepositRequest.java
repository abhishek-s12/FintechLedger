package com.abhishek.fintech.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Wallet Deposit Request")
public class DepositRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Deposit amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Amount can have at most 4 decimal places")
    @Schema(example = "1000.0000")
    private BigDecimal amount;

    @Schema(example = "Initial test deposit", defaultValue = "Wallet funding deposit")
    private String description;
}
