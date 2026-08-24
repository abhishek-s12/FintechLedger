package com.abhishek.fintech.ledger.repository;

import com.abhishek.fintech.ledger.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    Page<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(CASE WHEN e.entryType = com.abhishek.fintech.ledger.entity.EntryType.CREDIT THEN e.amount ELSE -e.amount END), 0.0000) " +
           "FROM LedgerEntry e WHERE e.wallet.id = :walletId")
    BigDecimal calculateWalletNetBalance(@Param("walletId") UUID walletId);
}
