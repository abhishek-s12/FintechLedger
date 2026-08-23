package com.abhishek.fintech.auth.dto;

import com.abhishek.fintech.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication Response containing JWT token and user profile")
public class AuthResponse {

    @Schema(description = "JWT Access Token")
    private String accessToken;

    @Schema(example = "Bearer", defaultValue = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Token expiration in milliseconds", example = "86400000")
    private long expiresIn;

    @Schema(description = "User unique identifier")
    private UUID userId;

    @Schema(example = "alice@example.com")
    private String email;

    @Schema(example = "Alice")
    private String firstName;

    @Schema(example = "Smith")
    private String lastName;

    @Schema(example = "ROLE_USER")
    private Role role;
}
