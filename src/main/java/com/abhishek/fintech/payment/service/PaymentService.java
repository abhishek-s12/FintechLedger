package com.abhishek.fintech.payment.service;

import com.abhishek.fintech.common.exception.ResourceNotFoundException;
import com.abhishek.fintech.idempotency.entity.IdempotencyKey;
import com.abhishek.fintech.idempotency.entity.IdempotencyStatus;
import com.abhishek.fintech.idempotency.service.IdempotencyService;
import com.abhishek.fintech.ledger.entity.LedgerTransaction;
import com.abhishek.fintech.ledger.service.LedgerService;
import com.abhishek.fintech.payment.dto.PaymentResponse;
import com.abhishek.fintech.payment.dto.TransferRequest;
import com.abhishek.fintech.payment.dto.TransferResponse;
import com.abhishek.fintech.payment.entity.Payment;
import com.abhishek.fintech.payment.entity.PaymentStatus;
import com.abhishek.fintech.payment.exception.*;
import com.abhishek.fintech.payment.repository.PaymentRepository;
import com.abhishek.fintech.user.entity.User;
import com.abhishek.fintech.wallet.entity.Wallet;
import com.abhishek.fintech.wallet.entity.WalletStatus;
import com.abhishek.fintech.wallet.exception.InvalidWalletStateException;
import com.abhishek.fintech.wallet.exception.WalletNotFoundException;
import com.abhishek.fintech.wallet.repository.WalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;
    private final com.abhishek.fintech.user.repository.UserRepository userRepository;
    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;
    private final com.abhishek.fintech.outbox.service.OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public TransferResponse transfer(UUID currentUserId, TransferRequest request, String idempotencyKeyHeader) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        // 1. Handle Idempotency Key if provided
        String payloadHash = idempotencyService.computePayloadHash(request);
        if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
            Optional<IdempotencyKey> existingKeyOpt = idempotencyService.findKey(idempotencyKeyHeader, currentUser.getId());
            if (existingKeyOpt.isPresent()) {
                IdempotencyKey key = existingKeyOpt.get();
                if (key.getStatus() == IdempotencyStatus.COMPLETED) {
                    try {
                        log.info("Returning cached response for idempotency key: {}", idempotencyKeyHeader);
                        return objectMapper.readValue(key.getResponseBody(), TransferResponse.class);
                    } catch (Exception e) {
                        log.error("Failed to parse cached idempotent response", e);
                    }
                }
            }
            idempotencyService.createOrValidateKey(idempotencyKeyHeader, currentUser, payloadHash);
        }

        try {
            TransferResponse response = executeTransferInternal(currentUser, request, idempotencyKeyHeader);

            if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
                idempotencyService.markCompleted(idempotencyKeyHeader, currentUser.getId(), 200, response);
            }

            return response;
        } catch (Exception ex) {
            if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
                idempotencyService.markFailed(idempotencyKeyHeader, currentUser.getId(), 400, ex.getMessage());
            }
            throw ex;
        }
    }

    private TransferResponse executeTransferInternal(User currentUser, TransferRequest request, String idempotencyKey) {
        UUID fromId = request.getFromWalletId();
        UUID toId = request.getToWalletId();

        if (fromId.equals(toId)) {
            throw new SelfTransferNotAllowedException("Cannot transfer funds to the same wallet");
        }

        // 2. Deadlock Prevention: Deterministic Lock Ordering by Wallet UUID
        UUID firstId = fromId.compareTo(toId) < 0 ? fromId : toId;
        UUID secondId = fromId.compareTo(toId) < 0 ? toId : fromId;

        Wallet firstWallet = walletRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + firstId));
        Wallet secondWallet = walletRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + secondId));

        Wallet senderWallet = fromId.equals(firstId) ? firstWallet : secondWallet;
        Wallet receiverWallet = toId.equals(firstId) ? firstWallet : secondWallet;

        // 3. Ownership & State Validations
        if (!senderWallet.getUser().getId().equals(currentUser.getId())) {
            throw new InvalidWalletStateException("Authenticated user does not own sender wallet: " + fromId);
        }

        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new InvalidWalletStateException("Sender wallet is in " + senderWallet.getStatus() + " state");
        }

        if (receiverWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new InvalidWalletStateException("Receiver wallet is in " + receiverWallet.getStatus() + " state");
        }

        String requestCurrency = request.getCurrency().trim().toUpperCase();
        if (!senderWallet.getCurrency().equalsIgnoreCase(requestCurrency)) {
            throw new CurrencyMismatchException(
                    String.format("Sender wallet currency [%s] does not match request currency [%s]",
                            senderWallet.getCurrency(), requestCurrency)
            );
        }

        if (!receiverWallet.getCurrency().equalsIgnoreCase(requestCurrency)) {
            throw new CurrencyMismatchException(
                    String.format("Receiver wallet currency [%s] does not match request currency [%s]",
                            receiverWallet.getCurrency(), requestCurrency)
            );
        }

        BigDecimal transferAmount = request.getAmount().setScale(4, RoundingMode.HALF_UP);
        if (senderWallet.getBalance().compareTo(transferAmount) < 0) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds in wallet [%s]. Available: %s %s, Requested: %s %s",
                            fromId, senderWallet.getBalance(), senderWallet.getCurrency(),
                            transferAmount, requestCurrency)
            );
        }

        // 4. Update Wallet Balances
        senderWallet.setBalance(senderWallet.getBalance().subtract(transferAmount));
        receiverWallet.setBalance(receiverWallet.getBalance().add(transferAmount));

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        // 5. Generate References
        String txRefId = "TX-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();
        String paymentRefId = "PAY-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();
        String description = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription()
                : "Transfer from " + fromId + " to " + toId;

        // 6. Record Double-Entry Ledger Transaction & Line Items
        LedgerTransaction ledgerTx = ledgerService.recordDoubleEntryTransfer(
                txRefId,
                description,
                senderWallet,
                receiverWallet,
                transferAmount,
                requestCurrency
        );

        // 7. Record Payment Entity
        Payment payment = Payment.builder()
                .referenceId(paymentRefId)
                .senderWallet(senderWallet)
                .receiverWallet(receiverWallet)
                .amount(transferAmount)
                .currency(requestCurrency)
                .status(PaymentStatus.COMPLETED)
                .idempotencyKey(idempotencyKey)
                .completedAt(Instant.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // 8. Record Outbox Event for reliable Kafka message publishing
        com.abhishek.fintech.messaging.event.PaymentCompletedEvent completedEvent =
                com.abhishek.fintech.messaging.event.PaymentCompletedEvent.builder()
                        .eventId("EVT-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase())
                        .eventType("PAYMENT_COMPLETED")
                        .paymentId(savedPayment.getId())
                        .referenceId(paymentRefId)
                        .senderWalletId(fromId)
                        .receiverWalletId(toId)
                        .senderUserId(senderWallet.getUser().getId())
                        .receiverUserId(receiverWallet.getUser().getId())
                        .amount(transferAmount)
                        .currency(requestCurrency)
                        .timestamp(savedPayment.getCreatedAt())
                        .build();

        outboxService.saveEvent("PAYMENT", savedPayment.getId().toString(), "PAYMENT_COMPLETED", completedEvent);

        log.info("Processed payment transfer [{}]: {} {} from wallet [{}] to [{}]",
                paymentRefId, transferAmount, requestCurrency, fromId, toId);

        return TransferResponse.builder()
                .paymentId(savedPayment.getId())
                .referenceId(paymentRefId)
                .fromWalletId(fromId)
                .toWalletId(toId)
                .amount(transferAmount)
                .currency(requestCurrency)
                .status(PaymentStatus.COMPLETED)
                .ledgerTransactionId(ledgerTx.getId())
                .description(description)
                .timestamp(savedPayment.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReferenceId(String referenceId, UUID currentUserId) {
        Payment payment = paymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with reference: " + referenceId));

        // Ensure user is party to this payment (or admin)
        boolean isSender = payment.getSenderWallet().getUser().getId().equals(currentUserId);
        boolean isReceiver = payment.getReceiverWallet().getUser().getId().equals(currentUserId);

        if (!isSender && !isReceiver) {
            throw new ResourceNotFoundException("Payment not found or access denied");
        }

        return mapToPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsForWallet(UUID walletId, UUID currentUserId, Pageable pageable) {
        Wallet wallet = walletRepository.findByIdAndUserId(walletId, currentUserId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));

        return paymentRepository.findByWalletId(wallet.getId(), pageable)
                .map(this::mapToPaymentResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsForUser(UUID userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable)
                .map(this::mapToPaymentResponse);
    }

    public PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .referenceId(payment.getReferenceId())
                .senderWalletId(payment.getSenderWallet().getId())
                .receiverWalletId(payment.getReceiverWallet().getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .idempotencyKey(payment.getIdempotencyKey())
                .errorMessage(payment.getErrorMessage())
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getCompletedAt())
                .build();
    }
}
