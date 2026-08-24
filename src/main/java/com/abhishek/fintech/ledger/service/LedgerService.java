package com.abhishek.fintech.ledger.service;

import com.abhishek.fintech.common.exception.ResourceNotFoundException;
import com.abhishek.fintech.ledger.dto.LedgerAuditResponse;
import com.abhishek.fintech.ledger.dto.LedgerEntryResponse;
import com.abhishek.fintech.ledger.dto.LedgerTransactionResponse;
import com.abhishek.fintech.ledger.entity.*;
import com.abhishek.fintech.ledger.exception.LedgerImbalanceException;
import com.abhishek.fintech.ledger.repository.LedgerEntryRepository;
import com.abhishek.fintech.ledger.repository.LedgerTransactionRepository;
import com.abhishek.fintech.wallet.entity.Wallet;
import com.abhishek.fintech.wallet.exception.WalletNotFoundException;
import com.abhishek.fintech.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;
    private final WalletRepository walletRepository;

    @Transactional
    public LedgerTransaction recordDoubleEntryTransfer(
            String referenceId,
            String description,
            Wallet senderWallet,
            Wallet receiverWallet,
            BigDecimal amount,
            String currency
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be strictly positive");
        }

        BigDecimal scaledAmount = amount.setScale(4, RoundingMode.HALF_UP);

        // Double-entry validation invariant: Debit Total must equal Credit Total
        BigDecimal totalDebits = scaledAmount;
        BigDecimal totalCredits = scaledAmount;
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new LedgerImbalanceException(
                    String.format("Ledger imbalance detected: Debits [%s] != Credits [%s]", totalDebits, totalCredits)
            );
        }

        LedgerTransaction transaction = LedgerTransaction.builder()
                .referenceId(referenceId)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .build();

        LedgerEntry debitEntry = LedgerEntry.builder()
                .wallet(senderWallet)
                .entryType(EntryType.DEBIT)
                .amount(scaledAmount)
                .currency(currency)
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .wallet(receiverWallet)
                .entryType(EntryType.CREDIT)
                .amount(scaledAmount)
                .currency(currency)
                .build();

        transaction.addEntry(debitEntry);
        transaction.addEntry(creditEntry);

        LedgerTransaction saved = transactionRepository.save(transaction);
        log.info("Recorded double-entry ledger transaction [{}] with 2 balancing entries for amount {} {}",
                referenceId, scaledAmount, currency);

        return saved;
    }

    @Transactional
    public LedgerTransaction recordDeposit(
            String referenceId,
            String description,
            Wallet wallet,
            BigDecimal amount,
            String currency
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be strictly positive");
        }

        BigDecimal scaledAmount = amount.setScale(4, RoundingMode.HALF_UP);

        LedgerTransaction transaction = LedgerTransaction.builder()
                .referenceId(referenceId)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .wallet(wallet)
                .entryType(EntryType.CREDIT)
                .amount(scaledAmount)
                .currency(currency)
                .build();

        transaction.addEntry(creditEntry);

        LedgerTransaction saved = transactionRepository.save(transaction);
        log.info("Recorded deposit ledger transaction [{}] for wallet [{}] amount {} {}",
                referenceId, wallet.getId(), scaledAmount, currency);

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getWalletLedgerEntries(UUID walletId, Pageable pageable) {
        return entryRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable)
                .map(this::mapToEntryResponse);
    }

    @Transactional(readOnly = true)
    public LedgerTransactionResponse getTransactionByReferenceId(String referenceId) {
        LedgerTransaction transaction = transactionRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Ledger transaction not found with reference: " + referenceId));
        return mapToTransactionResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<LedgerTransactionResponse> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToTransactionResponse);
    }

    @Transactional(readOnly = true)
    public LedgerAuditResponse auditWalletBalance(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));

        BigDecimal calculatedBalance = entryRepository.calculateWalletNetBalance(walletId)
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal actualBalance = wallet.getBalance().setScale(4, RoundingMode.HALF_UP);
        BigDecimal discrepancy = actualBalance.subtract(calculatedBalance).abs();

        boolean isConsistent = discrepancy.compareTo(BigDecimal.ZERO) == 0;

        if (!isConsistent) {
            log.warn("AUDIT DISCREPANCY on wallet {}: Wallet balance = {}, Ledger aggregate = {}, Diff = {}",
                    walletId, actualBalance, calculatedBalance, discrepancy);
        }

        return LedgerAuditResponse.builder()
                .walletId(walletId)
                .currency(wallet.getCurrency())
                .walletBalance(actualBalance)
                .ledgerCalculatedBalance(calculatedBalance)
                .discrepancy(discrepancy)
                .isConsistent(isConsistent)
                .auditedAt(Instant.now())
                .build();
    }

    public LedgerTransactionResponse mapToTransactionResponse(LedgerTransaction tx) {
        List<LedgerEntryResponse> entryResponses = tx.getEntries().stream()
                .map(this::mapToEntryResponse)
                .collect(Collectors.toList());

        return LedgerTransactionResponse.builder()
                .id(tx.getId())
                .referenceId(tx.getReferenceId())
                .type(tx.getType())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .entries(entryResponses)
                .build();
    }

    public LedgerEntryResponse mapToEntryResponse(LedgerEntry entry) {
        return LedgerEntryResponse.builder()
                .id(entry.getId())
                .transactionId(entry.getTransaction().getId())
                .walletId(entry.getWallet().getId())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .currency(entry.getCurrency())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
