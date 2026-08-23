package com.abhishek.fintech.auth.dto;

import com.abhishek.fintech.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Registration Request Payload")
public class RegisterRequest {

    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email format")
    @Schema(example = "alice@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Schema(example = "SecretPassword123!")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Schema(example = "Alice")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Schema(example = "Smith")
    private String lastName;

    @Schema(example = "ROLE_USER", defaultValue = "ROLE_USER")
    private Role role;
}
