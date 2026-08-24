package com.abhishek.fintech.idempotency.service;

import com.abhishek.fintech.idempotency.entity.IdempotencyKey;
import com.abhishek.fintech.idempotency.entity.IdempotencyStatus;
import com.abhishek.fintech.idempotency.exception.IdempotencyConflictException;
import com.abhishek.fintech.idempotency.repository.IdempotencyKeyRepository;
import com.abhishek.fintech.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public String computePayloadHash(Object payload) {
        try {
            String json = payload instanceof String ? (String) payload : objectMapper.writeValueAsString(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        } catch (Exception e) {
            log.error("Failed to serialize payload for hash calculation", e);
            return UUID.randomUUID().toString();
        }
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyKey> findKey(String keyValue, UUID userId) {
        return idempotencyKeyRepository.findByKeyValueAndUserId(keyValue, userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyKey createOrValidateKey(String keyValue, User user, String requestHash) {
        Optional<IdempotencyKey> existingOpt = idempotencyKeyRepository.findByKeyValueAndUserId(keyValue, user.getId());

        if (existingOpt.isPresent()) {
            IdempotencyKey existing = existingOpt.get();

            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "Idempotency key [" + keyValue + "] has already been used with a different request payload"
                );
            }

            if (existing.getStatus() == IdempotencyStatus.PROCESSING) {
                throw new IdempotencyConflictException(
                        "A request with idempotency key [" + keyValue + "] is currently processing. Please retry shortly."
                );
            }

            return existing;
        }

        IdempotencyKey newKey = IdempotencyKey.builder()
                .keyValue(keyValue)
                .user(user)
                .requestHash(requestHash)
                .status(IdempotencyStatus.PROCESSING)
                .build();

        return idempotencyKeyRepository.save(newKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String keyValue, UUID userId, int responseCode, Object responseBody) {
        try {
            String bodyJson = responseBody instanceof String ? (String) responseBody : objectMapper.writeValueAsString(responseBody);
            idempotencyKeyRepository.findByKeyValueAndUserId(keyValue, userId).ifPresent(key -> {
                key.setStatus(IdempotencyStatus.COMPLETED);
                key.setResponseCode(responseCode);
                key.setResponseBody(bodyJson);
                idempotencyKeyRepository.save(key);
                log.debug("Marked idempotency key [{}] as COMPLETED with status {}", keyValue, responseCode);
            });
        } catch (Exception e) {
            log.error("Failed to mark idempotency key as completed", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String keyValue, UUID userId, int responseCode, String errorMessage) {
        idempotencyKeyRepository.findByKeyValueAndUserId(keyValue, userId).ifPresent(key -> {
            key.setStatus(IdempotencyStatus.FAILED);
            key.setResponseCode(responseCode);
            key.setResponseBody(errorMessage);
            idempotencyKeyRepository.save(key);
            log.debug("Marked idempotency key [{}] as FAILED", keyValue);
        });
    }
}
