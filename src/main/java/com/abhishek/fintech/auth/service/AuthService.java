package com.abhishek.fintech.auth.service;

import com.abhishek.fintech.auth.dto.AuthResponse;
import com.abhishek.fintech.auth.dto.LoginRequest;
import com.abhishek.fintech.auth.dto.RegisterRequest;
import com.abhishek.fintech.common.exception.DuplicateResourceException;
import com.abhishek.fintech.common.exception.InvalidCredentialsException;
import com.abhishek.fintech.security.JwtService;
import com.abhishek.fintech.security.SecurityUser;
import com.abhishek.fintech.user.entity.Role;
import com.abhishek.fintech.user.entity.User;
import com.abhishek.fintech.user.entity.UserStatus;
import com.abhishek.fintech.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("User already exists with email: " + normalizedEmail);
        }

        Role assignedRole = request.getRole() != null ? request.getRole() : Role.ROLE_USER;

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(assignedRole)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with id: {}, email: {}, role: {}", savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        SecurityUser securityUser = new SecurityUser(savedUser);
        String token = jwtService.generateToken(securityUser);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTimeMs())
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .role(savedUser.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );

            SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
            User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

            String token = jwtService.generateToken(securityUser);
            log.info("User successfully logged in: {}", normalizedEmail);

            return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTimeMs())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", normalizedEmail);
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }
}
