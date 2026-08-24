package com.abhishek.fintech.payment.repository;

import com.abhishek.fintech.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByReferenceId(String referenceId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT p FROM Payment p WHERE p.senderWallet.id = :walletId OR p.receiverWallet.id = :walletId ORDER BY p.createdAt DESC")
    Page<Payment> findByWalletId(@Param("walletId") UUID walletId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE (p.senderWallet.user.id = :userId OR p.receiverWallet.user.id = :userId) ORDER BY p.createdAt DESC")
    Page<Payment> findByUserId(@Param("userId") UUID userId, Pageable pageable);
}
