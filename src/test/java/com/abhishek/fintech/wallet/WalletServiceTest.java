package com.abhishek.fintech.wallet;

import com.abhishek.fintech.common.exception.DuplicateResourceException;
import com.abhishek.fintech.user.entity.Role;
import com.abhishek.fintech.user.entity.User;
import com.abhishek.fintech.user.entity.UserStatus;
import com.abhishek.fintech.user.repository.UserRepository;
import com.abhishek.fintech.wallet.dto.CreateWalletRequest;
import com.abhishek.fintech.wallet.dto.DepositRequest;
import com.abhishek.fintech.wallet.dto.WalletBalanceResponse;
import com.abhishek.fintech.wallet.dto.WalletResponse;
import com.abhishek.fintech.wallet.entity.Wallet;
import com.abhishek.fintech.wallet.entity.WalletStatus;
import com.abhishek.fintech.wallet.exception.InvalidWalletStateException;
import com.abhishek.fintech.wallet.exception.WalletNotFoundException;
import com.abhishek.fintech.wallet.repository.WalletRepository;
import com.abhishek.fintech.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.abhishek.fintech.ledger.service.LedgerService ledgerService;

    @Mock
    private com.abhishek.fintech.outbox.service.OutboxService outboxService;

    @InjectMocks
    private WalletService walletService;

    private User sampleUser;
    private UUID userId;
    private Wallet sampleWallet;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .email("alice@fintech.com")
                .passwordHash("hashed")
                .firstName("Alice")
                .lastName("Smith")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        sampleUser.setId(userId);

        walletId = UUID.randomUUID();
        sampleWallet = Wallet.builder()
                .user(sampleUser)
                .currency("INR")
                .balance(BigDecimal.valueOf(1000).setScale(4, RoundingMode.HALF_UP))
                .status(WalletStatus.ACTIVE)
                .version(0L)
                .build();
        sampleWallet.setId(walletId);
    }

    @Test
    void shouldCreateWalletSuccessfully() {
        CreateWalletRequest request = CreateWalletRequest.builder().currency("INR").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(walletRepository.existsByUserIdAndCurrency(userId, "INR")).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(sampleWallet);

        WalletResponse response = walletService.createWallet(userId, request);

        assertNotNull(response);
        assertEquals("INR", response.getCurrency());
        assertEquals(userId, response.getUserId());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void shouldThrowExceptionWhenDuplicateWalletCurrencyCreated() {
        CreateWalletRequest request = CreateWalletRequest.builder().currency("INR").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(walletRepository.existsByUserIdAndCurrency(userId, "INR")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> walletService.createWallet(userId, request));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void shouldDepositSuccessfullyIntoActiveWallet() {
        DepositRequest request = DepositRequest.builder()
                .amount(BigDecimal.valueOf(500))
                .description("Test deposit")
                .build();

        when(walletRepository.findByIdAndUserId(walletId, userId)).thenReturn(Optional.of(sampleWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = walletService.deposit(walletId, userId, request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(1500).setScale(4, RoundingMode.HALF_UP), response.getBalance());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void shouldThrowExceptionWhenDepositingIntoFrozenWallet() {
        sampleWallet.setStatus(WalletStatus.FROZEN);
        DepositRequest request = DepositRequest.builder()
                .amount(BigDecimal.valueOf(500))
                .build();

        when(walletRepository.findByIdAndUserId(walletId, userId)).thenReturn(Optional.of(sampleWallet));

        assertThrows(InvalidWalletStateException.class, () -> walletService.deposit(walletId, userId, request));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void shouldFreezeAndUnfreezeWalletSuccessfully() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(sampleWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse frozen = walletService.freezeWallet(walletId);
        assertEquals(WalletStatus.FROZEN, frozen.getStatus());

        WalletResponse unfrozen = walletService.unfreezeWallet(walletId);
        assertEquals(WalletStatus.ACTIVE, unfrozen.getStatus());
    }

    @Test
    void shouldGetWalletBalance() {
        when(walletRepository.findByIdAndUserId(walletId, userId)).thenReturn(Optional.of(sampleWallet));

        WalletBalanceResponse balanceResponse = walletService.getWalletBalance(walletId, userId);

        assertNotNull(balanceResponse);
        assertEquals(walletId, balanceResponse.getWalletId());
        assertEquals("INR", balanceResponse.getCurrency());
        assertEquals(BigDecimal.valueOf(1000).setScale(4, RoundingMode.HALF_UP), balanceResponse.getBalance());
    }
}
