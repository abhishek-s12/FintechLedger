package com.abhishek.fintech.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login Request Payload")
public class LoginRequest {

    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email format")
    @Schema(example = "alice@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(example = "SecretPassword123!")
    private String password;
}
