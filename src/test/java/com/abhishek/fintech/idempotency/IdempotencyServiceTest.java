package com.abhishek.fintech.idempotency;

import com.abhishek.fintech.idempotency.entity.IdempotencyKey;
import com.abhishek.fintech.idempotency.entity.IdempotencyStatus;
import com.abhishek.fintech.idempotency.exception.IdempotencyConflictException;
import com.abhishek.fintech.idempotency.repository.IdempotencyKeyRepository;
import com.abhishek.fintech.idempotency.service.IdempotencyService;
import com.abhishek.fintech.user.entity.Role;
import com.abhishek.fintech.user.entity.User;
import com.abhishek.fintech.user.entity.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private IdempotencyService idempotencyService;

    private User sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .email("alice@fintech.com")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        sampleUser.setId(userId);
    }

    @Test
    void shouldCreateNewIdempotencyKey() {
        when(idempotencyKeyRepository.findByKeyValueAndUserId("key-123", userId)).thenReturn(Optional.empty());
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IdempotencyKey key = idempotencyService.createOrValidateKey("key-123", sampleUser, "hash-abc");

        assertNotNull(key);
        assertEquals("key-123", key.getKeyValue());
        assertEquals("hash-abc", key.getRequestHash());
        assertEquals(IdempotencyStatus.PROCESSING, key.getStatus());
    }

    @Test
    void shouldReturnExistingCompletedKeyWhenHashesMatch() {
        IdempotencyKey existingKey = IdempotencyKey.builder()
                .keyValue("key-123")
                .user(sampleUser)
                .requestHash("hash-abc")
                .status(IdempotencyStatus.COMPLETED)
                .responseCode(200)
                .responseBody("{\"status\":\"OK\"}")
                .build();

        when(idempotencyKeyRepository.findByKeyValueAndUserId("key-123", userId)).thenReturn(Optional.of(existingKey));

        IdempotencyKey result = idempotencyService.createOrValidateKey("key-123", sampleUser, "hash-abc");

        assertNotNull(result);
        assertEquals(IdempotencyStatus.COMPLETED, result.getStatus());
        assertEquals(200, result.getResponseCode());
    }

    @Test
    void shouldThrowConflictWhenPayloadHashDiffers() {
        IdempotencyKey existingKey = IdempotencyKey.builder()
                .keyValue("key-123")
                .user(sampleUser)
                .requestHash("hash-abc")
                .status(IdempotencyStatus.COMPLETED)
                .build();

        when(idempotencyKeyRepository.findByKeyValueAndUserId("key-123", userId)).thenReturn(Optional.of(existingKey));

        assertThrows(IdempotencyConflictException.class, () ->
                idempotencyService.createOrValidateKey("key-123", sampleUser, "hash-different")
        );
    }

    @Test
    void shouldThrowConflictWhenKeyIsCurrentlyProcessing() {
        IdempotencyKey existingKey = IdempotencyKey.builder()
                .keyValue("key-123")
                .user(sampleUser)
                .requestHash("hash-abc")
                .status(IdempotencyStatus.PROCESSING)
                .build();

        when(idempotencyKeyRepository.findByKeyValueAndUserId("key-123", userId)).thenReturn(Optional.of(existingKey));

        assertThrows(IdempotencyConflictException.class, () ->
                idempotencyService.createOrValidateKey("key-123", sampleUser, "hash-abc")
        );
    }
}
