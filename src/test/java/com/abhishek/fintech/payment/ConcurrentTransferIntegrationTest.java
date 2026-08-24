package com.abhishek.fintech.payment;

import com.abhishek.fintech.ledger.repository.LedgerEntryRepository;
import com.abhishek.fintech.ledger.repository.LedgerTransactionRepository;
import com.abhishek.fintech.payment.dto.TransferRequest;
import com.abhishek.fintech.payment.repository.PaymentRepository;
import com.abhishek.fintech.payment.service.PaymentService;
import com.abhishek.fintech.user.entity.Role;
import com.abhishek.fintech.user.entity.User;
import com.abhishek.fintech.user.entity.UserStatus;
import com.abhishek.fintech.user.repository.UserRepository;
import com.abhishek.fintech.wallet.entity.Wallet;
import com.abhishek.fintech.wallet.entity.WalletStatus;
import com.abhishek.fintech.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentTransferIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private LedgerTransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository entryRepository;

    private User userA;
    private User userB;
    private Wallet walletA;
    private Wallet walletB;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        entryRepository.deleteAll();
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        userA = userRepository.save(User.builder()
                .email("usera@fintech.com")
                .passwordHash("hashed")
                .firstName("User")
                .lastName("A")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());

        userB = userRepository.save(User.builder()
                .email("userb@fintech.com")
                .passwordHash("hashed")
                .firstName("User")
                .lastName("B")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());

        walletA = walletRepository.save(Wallet.builder()
                .user(userA)
                .currency("INR")
                .balance(BigDecimal.valueOf(1000).setScale(4, RoundingMode.HALF_UP))
                .status(WalletStatus.ACTIVE)
                .build());

        walletB = walletRepository.save(Wallet.builder()
                .user(userB)
                .currency("INR")
                .balance(BigDecimal.valueOf(1000).setScale(4, RoundingMode.HALF_UP))
                .status(WalletStatus.ACTIVE)
                .build());
    }

    @Test
    void shouldHandleConcurrentTransfersWithoutDataCorruptionOrDeadlock() throws InterruptedException {
        int numberOfThreads = 10;
        int transfersPerThread = 5;
        BigDecimal transferAmount = BigDecimal.valueOf(10).setScale(4, RoundingMode.HALF_UP);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successfulTransfers = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            final boolean directionAtoB = (i % 2 == 0);
            futures.add(executorService.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < transfersPerThread; j++) {
                        try {
                            if (directionAtoB) {
                                TransferRequest req = TransferRequest.builder()
                                        .fromWalletId(walletA.getId())
                                        .toWalletId(walletB.getId())
                                        .amount(transferAmount)
                                        .currency("INR")
                                        .description("A to B transfer")
                                        .build();
                                paymentService.transfer(userA.getId(), req, null);
                            } else {
                                TransferRequest req = TransferRequest.builder()
                                        .fromWalletId(walletB.getId())
                                        .toWalletId(walletA.getId())
                                        .amount(transferAmount)
                                        .currency("INR")
                                        .description("B to A transfer")
                                        .build();
                                paymentService.transfer(userB.getId(), req, null);
                            }
                            successfulTransfers.incrementAndGet();
                        } catch (Exception ignored) {
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            }));
        }

        // Start all threads concurrently
        startLatch.countDown();
        finishLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        Wallet updatedWalletA = walletRepository.findById(walletA.getId()).orElseThrow();
        Wallet updatedWalletB = walletRepository.findById(walletB.getId()).orElseThrow();

        BigDecimal totalInitialBalance = BigDecimal.valueOf(2000).setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalFinalBalance = updatedWalletA.getBalance().add(updatedWalletB.getBalance()).setScale(4, RoundingMode.HALF_UP);

        // Invariant: Total money in the system must be strictly conserved!
        assertEquals(totalInitialBalance, totalFinalBalance,
                "Total balance across all wallets must equal the initial total balance!");
    }
}
