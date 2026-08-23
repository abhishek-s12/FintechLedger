package com.abhishek.fintech.user.dto;

import com.abhishek.fintech.user.entity.Role;
import com.abhishek.fintech.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User Profile Response")
public class UserResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private UserStatus status;
    private Instant createdAt;
}
