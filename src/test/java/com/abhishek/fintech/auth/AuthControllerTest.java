package com.abhishek.fintech.auth;

import com.abhishek.fintech.auth.controller.AuthController;
import com.abhishek.fintech.auth.dto.AuthResponse;
import com.abhishek.fintech.auth.dto.LoginRequest;
import com.abhishek.fintech.auth.dto.RegisterRequest;
import com.abhishek.fintech.auth.service.AuthService;
import com.abhishek.fintech.common.exception.DuplicateResourceException;
import com.abhishek.fintech.common.exception.GlobalExceptionHandler;
import com.abhishek.fintech.common.exception.InvalidCredentialsException;
import com.abhishek.fintech.security.JwtAuthenticationFilter;
import com.abhishek.fintech.security.JwtService;
import com.abhishek.fintech.user.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("alice@fintech.com")
                .password("Password123!")
                .firstName("Alice")
                .lastName("Smith")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("test-jwt-token")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .userId(UUID.randomUUID())
                .email("alice@fintech.com")
                .firstName("Alice")
                .lastName("Smith")
                .role(Role.ROLE_USER)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("test-jwt-token"))
                .andExpect(jsonPath("$.email").value("alice@fintech.com"))
                .andExpect(jsonPath("$.firstName").value("Alice"));
    }

    @Test
    void shouldReturnBadRequestWhenValidationFails() throws Exception {
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .email("invalid-email")
                .password("short")
                .firstName("")
                .lastName("")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.invalidParams").exists());
    }

    @Test
    void shouldReturnConflictWhenEmailExists() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("alice@fintech.com")
                .password("Password123!")
                .firstName("Alice")
                .lastName("Smith")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("User already exists with email: alice@fintech.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("alice@fintech.com")
                .password("Password123!")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("test-jwt-token")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .userId(UUID.randomUUID())
                .email("alice@fintech.com")
                .firstName("Alice")
                .lastName("Smith")
                .role(Role.ROLE_USER)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-jwt-token"))
                .andExpect(jsonPath("$.email").value("alice@fintech.com"));
    }

    @Test
    void shouldReturnUnauthorizedOnInvalidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("alice@fintech.com")
                .password("WrongPassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication Failed"));
    }
}
