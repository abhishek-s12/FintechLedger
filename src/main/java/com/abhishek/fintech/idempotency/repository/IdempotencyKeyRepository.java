package com.abhishek.fintech.idempotency.repository;

import com.abhishek.fintech.idempotency.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByKeyValue(String keyValue);

    Optional<IdempotencyKey> findByKeyValueAndUserId(String keyValue, UUID userId);
}
