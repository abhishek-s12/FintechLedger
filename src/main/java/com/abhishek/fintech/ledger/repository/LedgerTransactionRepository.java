package com.abhishek.fintech.ledger.repository;

import com.abhishek.fintech.ledger.entity.LedgerTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, UUID> {

    Optional<LedgerTransaction> findByReferenceId(String referenceId);

    boolean existsByReferenceId(String referenceId);

    Page<LedgerTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
