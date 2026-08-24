package com.abhishek.fintech.payment;

import com.abhishek.fintech.idempotency.service.IdempotencyService;
import com.abhishek.fintech.ledger.entity.LedgerTransaction;
import com.abhishek.fintech.ledger.service.LedgerService;
import com.abhishek.fintech.payment.dto.TransferRequest;
import com.abhishek.fintech.payment.dto.TransferResponse;
import com.abhishek.fintech.payment.entity.Payment;
import com.abhishek.fintech.payment.entity.PaymentStatus;
import com.abhishek.fintech.payment.exception.CurrencyMismatchException;
import com.abhishek.fintech.payment.exception.InsufficientFundsException;
import com.abhishek.fintech.payment.exception.SelfTransferNotAllowedException;
import com.abhishek.fintech.payment.repository.PaymentRepository;
import com.abhishek.fintech.payment.service.PaymentService;
import com.abhishek.fintech.user.entity.Role;
import com.abhishek.fintech.user.entity.User;
import com.abhishek.fintech.user.entity.UserStatus;
import com.abhishek.fintech.wallet.entity.Wallet;
import com.abhishek.fintech.wallet.entity.WalletStatus;
import com.abhishek.fintech.wallet.exception.InvalidWalletStateException;
import com.abhishek.fintech.wallet.repository.WalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private com.abhishek.fintech.user.repository.UserRepository userRepository;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private IdempotencyService idempotencyService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentService paymentService;

    private User senderUser;
    private User receiverUser;
    private Wallet senderWallet;
    private Wallet receiverWallet;
    private UUID senderWalletId;
    private UUID receiverWalletId;

    @BeforeEach
    void setUp() {
        senderUser = User.builder()
                .email("alice@fintech.com")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        senderUser.setId(UUID.randomUUID());

        receiverUser = User.builder()
                .email("bob@fintech.com")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        receiverUser.setId(UUID.randomUUID());

        senderWalletId = UUID.randomUUID();
        receiverWalletId = UUID.randomUUID();

        // Ensure different IDs
        while (senderWalletId.equals(receiverWalletId)) {
            receiverWalletId = UUID.randomUUID();
        }

        senderWallet = Wallet.builder()
                .user(senderUser)
                .currency("INR")
                .balance(BigDecimal.valueOf(1000).setScale(4, RoundingMode.HALF_UP))
                .status(WalletStatus.ACTIVE)
                .build();
        senderWallet.setId(senderWalletId);

        receiverWallet = Wallet.builder()
                .user(receiverUser)
                .currency("INR")
                .balance(BigDecimal.valueOf(500).setScale(4, RoundingMode.HALF_UP))
                .status(WalletStatus.ACTIVE)
                .build();
        receiverWallet.setId(receiverWalletId);
    }

    @Test
    void shouldExecuteTransferSuccessfully() {
        TransferRequest request = TransferRequest.builder()
                .fromWalletId(senderWalletId)
                .toWalletId(receiverWalletId)
                .amount(BigDecimal.valueOf(300))
                .currency("INR")
                .description("Rent payment")
                .build();

        when(userRepository.findById(senderUser.getId())).thenReturn(Optional.of(senderUser));
        when(walletRepository.findByIdForUpdate(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(receiverWalletId)).thenReturn(Optional.of(receiverWallet));

        LedgerTransaction dummyTx = LedgerTransaction.builder()
                .id(UUID.randomUUID())
                .referenceId("TX-123")
                .build();
        when(ledgerService.recordDoubleEntryTransfer(any(), any(), any(), any(), any(), any()))
                .thenReturn(dummyTx);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            return p;
        });

        TransferResponse response = paymentService.transfer(senderUser.getId(), request, null);

        assertNotNull(response);
        assertEquals(PaymentStatus.COMPLETED, response.getStatus());
        assertEquals(BigDecimal.valueOf(300).setScale(4, RoundingMode.HALF_UP), response.getAmount());
        assertEquals("INR", response.getCurrency());
        assertEquals(BigDecimal.valueOf(700).setScale(4, RoundingMode.HALF_UP), senderWallet.getBalance());
        assertEquals(BigDecimal.valueOf(800).setScale(4, RoundingMode.HALF_UP), receiverWallet.getBalance());

        verify(walletRepository).save(senderWallet);
        verify(walletRepository).save(receiverWallet);
        verify(ledgerService).recordDoubleEntryTransfer(any(), any(), eq(senderWallet), eq(receiverWallet), eq(BigDecimal.valueOf(300).setScale(4, RoundingMode.HALF_UP)), eq("INR"));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void shouldThrowExceptionOnSelfTransfer() {
        TransferRequest request = TransferRequest.builder()
                .fromWalletId(senderWalletId)
                .toWalletId(senderWalletId)
                .amount(BigDecimal.valueOf(100))
                .currency("INR")
                .build();

        when(userRepository.findById(senderUser.getId())).thenReturn(Optional.of(senderUser));

        assertThrows(SelfTransferNotAllowedException.class, () ->
                paymentService.transfer(senderUser.getId(), request, null)
        );
    }

    @Test
    void shouldThrowExceptionWhenInsufficientFunds() {
        TransferRequest request = TransferRequest.builder()
                .fromWalletId(senderWalletId)
                .toWalletId(receiverWalletId)
                .amount(BigDecimal.valueOf(2000))
                .currency("INR")
                .build();

        when(userRepository.findById(senderUser.getId())).thenReturn(Optional.of(senderUser));
        when(walletRepository.findByIdForUpdate(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(receiverWalletId)).thenReturn(Optional.of(receiverWallet));

        assertThrows(InsufficientFundsException.class, () ->
                paymentService.transfer(senderUser.getId(), request, null)
        );

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldThrowExceptionWhenCurrencyMismatch() {
        receiverWallet.setCurrency("USD");

        TransferRequest request = TransferRequest.builder()
                .fromWalletId(senderWalletId)
                .toWalletId(receiverWalletId)
                .amount(BigDecimal.valueOf(100))
                .currency("INR")
                .build();

        when(userRepository.findById(senderUser.getId())).thenReturn(Optional.of(senderUser));
        when(walletRepository.findByIdForUpdate(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(receiverWalletId)).thenReturn(Optional.of(receiverWallet));

        assertThrows(CurrencyMismatchException.class, () ->
                paymentService.transfer(senderUser.getId(), request, null)
        );
    }

    @Test
    void shouldThrowExceptionWhenWalletIsFrozen() {
        senderWallet.setStatus(WalletStatus.FROZEN);

        TransferRequest request = TransferRequest.builder()
                .fromWalletId(senderWalletId)
                .toWalletId(receiverWalletId)
                .amount(BigDecimal.valueOf(100))
                .currency("INR")
                .build();

        when(userRepository.findById(senderUser.getId())).thenReturn(Optional.of(senderUser));
        when(walletRepository.findByIdForUpdate(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(receiverWalletId)).thenReturn(Optional.of(receiverWallet));

        assertThrows(InvalidWalletStateException.class, () ->
                paymentService.transfer(senderUser.getId(), request, null)
        );
    }
}
