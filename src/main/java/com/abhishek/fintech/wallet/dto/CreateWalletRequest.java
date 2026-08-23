package com.abhishek.fintech.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Wallet Creation Request")
public class CreateWalletRequest {

    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code (e.g. INR, USD, EUR)")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be uppercase 3-letter alphabetic code")
    @Schema(example = "INR", defaultValue = "INR")
    private String currency;
}
