package com.abhishek.fintech.auth;

import com.abhishek.fintech.auth.dto.AuthResponse;
import com.abhishek.fintech.auth.dto.LoginRequest;
import com.abhishek.fintech.auth.dto.RegisterRequest;
import com.abhishek.fintech.auth.service.AuthService;
import com.abhishek.fintech.common.exception.DuplicateResourceException;
import com.abhishek.fintech.common.exception.InvalidCredentialsException;
import com.abhishek.fintech.security.JwtService;
import com.abhishek.fintech.security.SecurityUser;
import com.abhishek.fintech.user.entity.Role;
import com.abhishek.fintech.user.entity.User;
import com.abhishek.fintech.user.entity.UserStatus;
import com.abhishek.fintech.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private UUID sampleUserId;

    @BeforeEach
    void setUp() {
        sampleUserId = UUID.randomUUID();
        sampleUser = User.builder()
                .email("alice@fintech.com")
                .passwordHash("encodedPassword")
                .firstName("Alice")
                .lastName("Smith")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        sampleUser.setId(sampleUserId);
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = RegisterRequest.builder()
                .email("alice@fintech.com")
                .password("Password123!")
                .firstName("Alice")
                .lastName("Smith")
                .build();

        when(userRepository.existsByEmailIgnoreCase("alice@fintech.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(any(SecurityUser.class))).thenReturn("mock-jwt-token");
        when(jwtService.getExpirationTimeMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getAccessToken());
        assertEquals("alice@fintech.com", response.getEmail());
        assertEquals("Alice", response.getFirstName());
        assertEquals(Role.ROLE_USER, response.getRole());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenRegisteringExistingEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .email("alice@fintech.com")
                .password("Password123!")
                .firstName("Alice")
                .lastName("Smith")
                .build();

        when(userRepository.existsByEmailIgnoreCase("alice@fintech.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = LoginRequest.builder()
                .email("alice@fintech.com")
                .password("Password123!")
                .build();

        SecurityUser securityUser = new SecurityUser(sampleUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities()));
        when(userRepository.findByEmailIgnoreCase("alice@fintech.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(any(SecurityUser.class))).thenReturn("mock-jwt-token");
        when(jwtService.getExpirationTimeMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getAccessToken());
        assertEquals("alice@fintech.com", response.getEmail());
    }

    @Test
    void shouldThrowExceptionOnInvalidLoginCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("alice@fintech.com")
                .password("WrongPassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }
}
