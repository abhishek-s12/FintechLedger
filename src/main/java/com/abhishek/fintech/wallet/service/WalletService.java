package com.abhishek.fintech.wallet.service;

import com.abhishek.fintech.common.exception.DuplicateResourceException;
import com.abhishek.fintech.common.exception.ResourceNotFoundException;
import com.abhishek.fintech.user.entity.User;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final com.abhishek.fintech.ledger.service.LedgerService ledgerService;
    private final com.abhishek.fintech.outbox.service.OutboxService outboxService;

    @Transactional
    public WalletResponse createWallet(UUID userId, CreateWalletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String currency = request.getCurrency().trim().toUpperCase();

        if (walletRepository.existsByUserIdAndCurrency(userId, currency)) {
            throw new DuplicateResourceException("Wallet with currency " + currency + " already exists for user");
        }

        Wallet wallet = Wallet.builder()
                .user(user)
                .currency(currency)
                .balance(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .status(WalletStatus.ACTIVE)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        // Record WALLET_CREATED Outbox event
        com.abhishek.fintech.messaging.event.WalletCreatedEvent createdEvent =
                com.abhishek.fintech.messaging.event.WalletCreatedEvent.builder()
                        .eventId("EVT-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase())
                        .eventType("WALLET_CREATED")
                        .walletId(savedWallet.getId())
                        .userId(userId)
                        .currency(currency)
                        .timestamp(savedWallet.getCreatedAt())
                        .build();
        outboxService.saveEvent("WALLET", savedWallet.getId().toString(), "WALLET_CREATED", createdEvent);

        log.info("Created new {} wallet with id: {} for user: {}", currency, savedWallet.getId(), userId);

        return mapToResponse(savedWallet);
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> getWalletsByUserId(UUID userId) {
        return walletRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletById(UUID walletId, UUID userId) {
        Wallet wallet = walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));
        return mapToResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletBalanceResponse getWalletBalance(UUID walletId, UUID userId) {
        Wallet wallet = walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));

        return WalletBalanceResponse.builder()
                .walletId(wallet.getId())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .status(wallet.getStatus())
                .build();
    }

    @Transactional
    public WalletResponse deposit(UUID walletId, UUID userId, DepositRequest request) {
        Wallet wallet = walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new InvalidWalletStateException("Cannot deposit into wallet in " + wallet.getStatus() + " state");
        }

        BigDecimal depositAmount = request.getAmount().setScale(4, RoundingMode.HALF_UP);
        wallet.setBalance(wallet.getBalance().add(depositAmount));

        Wallet updatedWallet = walletRepository.save(wallet);

        // Record deposit into ledger
        String refId = "DEP-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();
        String description = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription()
                : "Deposit of " + depositAmount + " " + wallet.getCurrency();
        ledgerService.recordDeposit(refId, description, updatedWallet, depositAmount, wallet.getCurrency());

        log.info("Deposited {} {} into wallet: {}", depositAmount, wallet.getCurrency(), walletId);

        return mapToResponse(updatedWallet);
    }

    @Transactional
    public WalletResponse freezeWallet(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));

        if (wallet.getStatus() == WalletStatus.CLOSED) {
            throw new InvalidWalletStateException("Cannot freeze a closed wallet");
        }

        wallet.setStatus(WalletStatus.FROZEN);
        Wallet updated = walletRepository.save(wallet);
        log.warn("Wallet {} has been FROZEN by admin", walletId);

        return mapToResponse(updated);
    }

    @Transactional
    public WalletResponse unfreezeWallet(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));

        if (wallet.getStatus() == WalletStatus.CLOSED) {
            throw new InvalidWalletStateException("Cannot unfreeze a closed wallet");
        }

        wallet.setStatus(WalletStatus.ACTIVE);
        Wallet updated = walletRepository.save(wallet);
        log.info("Wallet {} has been UNFROZEN by admin", walletId);

        return mapToResponse(updated);
    }

    public WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUser().getId())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .status(wallet.getStatus())
                .version(wallet.getVersion())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
