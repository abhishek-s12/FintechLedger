package com.abhishek.fintech.security;

import com.abhishek.fintech.user.entity.Role;
import com.abhishek.fintech.user.entity.User;
import com.abhishek.fintech.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);
    }

    @Test
    void shouldGenerateAndValidateTokenSuccessfully() {
        User user = User.builder()
                .email("alice@fintech.com")
                .passwordHash("hashedPass")
                .firstName("Alice")
                .lastName("Smith")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(UUID.randomUUID());

        SecurityUser securityUser = new SecurityUser(user);
        String token = jwtService.generateToken(securityUser);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        assertEquals("alice@fintech.com", jwtService.extractUsername(token));
        assertEquals(user.getId(), jwtService.extractUserId(token));
        assertTrue(jwtService.isTokenValid(token, securityUser));
        assertFalse(jwtService.isTokenExpired(token));
    }
}
