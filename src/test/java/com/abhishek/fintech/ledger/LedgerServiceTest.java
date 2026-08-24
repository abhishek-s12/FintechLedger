package com.abhishek.fintech.ledger;

import com.abhishek.fintech.ledger.dto.LedgerAuditResponse;
import com.abhishek.fintech.ledger.dto.LedgerTransactionResponse;
import com.abhishek.fintech.ledger.entity.*;
import com.abhishek.fintech.ledger.repository.LedgerEntryRepository;
import com.abhishek.fintech.ledger.repository.LedgerTransactionRepository;
import com.abhishek.fintech.ledger.service.LedgerService;
import com.abhishek.fintech.user.entity.Role;
import com.abhishek.fintech.user.entity.User;
import com.abhishek.fintech.user.entity.UserStatus;
import com.abhishek.fintech.wallet.entity.Wallet;
import com.abhishek.fintech.wallet.entity.WalletStatus;
import com.abhishek.fintech.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerTransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository entryRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private LedgerService ledgerService;

    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        User user1 = User.builder()
                .email("user1@fintech.com")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        user1.setId(UUID.randomUUID());

        User user2 = User.builder()
                .email("user2@fintech.com")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        user2.setId(UUID.randomUUID());

        senderWallet = Wallet.builder()
                .user(user1)
                .currency("INR")
                .balance(BigDecimal.valueOf(1000).setScale(4, RoundingMode.HALF_UP))
                .status(WalletStatus.ACTIVE)
                .build();
        senderWallet.setId(UUID.randomUUID());

        receiverWallet = Wallet.builder()
                .user(user2)
                .currency("INR")
                .balance(BigDecimal.valueOf(500).setScale(4, RoundingMode.HALF_UP))
                .status(WalletStatus.ACTIVE)
                .build();
        receiverWallet.setId(UUID.randomUUID());
    }

    @Test
    void shouldRecordDoubleEntryTransferSuccessfully() {
        when(transactionRepository.save(any(LedgerTransaction.class))).thenAnswer(invocation -> {
            LedgerTransaction tx = invocation.getArgument(0);
            tx.setId(UUID.randomUUID());
            tx.setCreatedAt(Instant.now());
            return tx;
        });

        LedgerTransaction result = ledgerService.recordDoubleEntryTransfer(
                "TX-12345",
                "Test transfer",
                senderWallet,
                receiverWallet,
                BigDecimal.valueOf(250),
                "INR"
        );

        assertNotNull(result);
        assertEquals("TX-12345", result.getReferenceId());
        assertEquals(2, result.getEntries().size());

        LedgerEntry debitEntry = result.getEntries().stream()
                .filter(e -> e.getEntryType() == EntryType.DEBIT)
                .findFirst()
                .orElse(null);
        assertNotNull(debitEntry);
        assertEquals(senderWallet, debitEntry.getWallet());
        assertEquals(BigDecimal.valueOf(250).setScale(4, RoundingMode.HALF_UP), debitEntry.getAmount());

        LedgerEntry creditEntry = result.getEntries().stream()
                .filter(e -> e.getEntryType() == EntryType.CREDIT)
                .findFirst()
                .orElse(null);
        assertNotNull(creditEntry);
        assertEquals(receiverWallet, creditEntry.getWallet());
        assertEquals(BigDecimal.valueOf(250).setScale(4, RoundingMode.HALF_UP), creditEntry.getAmount());

        verify(transactionRepository).save(any(LedgerTransaction.class));
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNegativeOrZero() {
        assertThrows(IllegalArgumentException.class, () ->
                ledgerService.recordDoubleEntryTransfer(
                        "TX-BAD",
                        "Bad amount",
                        senderWallet,
                        receiverWallet,
                        BigDecimal.ZERO,
                        "INR"
                )
        );
    }

    @Test
    void shouldAuditWalletBalanceAndConfirmConsistency() {
        UUID walletId = senderWallet.getId();
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(senderWallet));
        when(entryRepository.calculateWalletNetBalance(walletId))
                .thenReturn(BigDecimal.valueOf(1000).setScale(4, RoundingMode.HALF_UP));

        LedgerAuditResponse audit = ledgerService.auditWalletBalance(walletId);

        assertNotNull(audit);
        assertTrue(audit.isConsistent());
        assertEquals(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), audit.getDiscrepancy());
        assertEquals(BigDecimal.valueOf(1000).setScale(4, RoundingMode.HALF_UP), audit.getWalletBalance());
        assertEquals(BigDecimal.valueOf(1000).setScale(4, RoundingMode.HALF_UP), audit.getLedgerCalculatedBalance());
    }

    @Test
    void shouldAuditWalletBalanceAndDetectDiscrepancy() {
        UUID walletId = senderWallet.getId();
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(senderWallet));
        when(entryRepository.calculateWalletNetBalance(walletId))
                .thenReturn(BigDecimal.valueOf(950).setScale(4, RoundingMode.HALF_UP));

        LedgerAuditResponse audit = ledgerService.auditWalletBalance(walletId);

        assertNotNull(audit);
        assertFalse(audit.isConsistent());
        assertEquals(BigDecimal.valueOf(50).setScale(4, RoundingMode.HALF_UP), audit.getDiscrepancy());
    }
}
