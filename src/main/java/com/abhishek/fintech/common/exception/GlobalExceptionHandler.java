package com.abhishek.fintech.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String BASE_ERROR_URL = "https://api.fintech.com/errors/";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed. Please check field errors."
        );
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create(BASE_ERROR_URL + "validation-error"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        problemDetail.setProperty("invalidParams", fieldErrors);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResourceException(
            DuplicateResourceException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Resource Conflict");
        problemDetail.setType(URI.create(BASE_ERROR_URL + "resource-conflict"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create(BASE_ERROR_URL + "resource-not-found"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(com.abhishek.fintech.wallet.exception.WalletNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleWalletNotFoundException(
            com.abhishek.fintech.wallet.exception.WalletNotFoundException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Wallet Not Found");
        problemDetail.setType(URI.create(BASE_ERROR_URL + "wallet-not-found"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(com.abhishek.fintech.wallet.exception.InvalidWalletStateException.class)
    public ResponseEntity<ProblemDetail> handleInvalidWalletStateException(
            com.abhishek.fintech.wallet.exception.InvalidWalletStateException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problemDetail.setTitle("Invalid Wallet State");
        problemDetail.setType(URI.create(BASE_ERROR_URL + "invalid-wallet-state"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail);
    }

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ProblemDetail> handleAuthenticationException(
            Exception ex,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
        );
        problemDetail.setTitle("Authentication Failed");
        problemDetail.setType(URI.create(BASE_ERROR_URL + "invalid-credentials"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    @ExceptionHandler(com.abhishek.fintech.payment.exception.InsufficientFundsException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientFundsException(
            com.abhishek.fintech.payment.exception.InsufficientFundsException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problemDetail.setTitle("Insufficient Funds");
        problemDetail.setType(URI.create(BASE_ERROR_URL + "insufficient-funds"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail);
    }

    @ExceptionHandler(com.abhishek.fintech.payment.exception.PaymentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handlePaymentNotFoundException(
            com.abhishek.fintech.payment.exception.PaymentNotFoundException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Payment Not Found");
        problemDetail.setType(URI.create(BASE_ERROR_URL + "payment-not-found"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler({
            com.abhishek.fintech.payment.exception.SelfTransferNotAllowedException.class,
            com.abhishek.fintech.payment.exception.CurrencyMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequestExceptions(
            Exception ex,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create(BASE_ERROR_URL + "bad-request"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(com.abhishek.fintech.idempotency.exception.IdempotencyConflictException.class)
    public ResponseEntity<ProblemDetail> handleIdempotencyConflictException(
            com.abhishek.fintech.idempotency.exception.IdempotencyConflictException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Idempotency Conflict");
        problemDetail.setType(URI.create(BASE_ERROR_URL + "idempotency-conflict"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(com.abhishek.fintech.ledger.exception.LedgerImbalanceException.class)
    public ResponseEntity<ProblemDetail> handleLedgerImbalanceException(
            com.abhishek.fintech.ledger.exception.LedgerImbalanceException ex,
            HttpServletRequest request
    ) {
        String traceId = UUID.randomUUID().toString();
        log.error("CRITICAL LEDGER IMBALANCE [traceId={}]: {}", traceId, ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problemDetail.setTitle("Ledger Imbalance");
        problemDetail.setType(URI.create(BASE_ERROR_URL + "ledger-imbalance"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action"
        );
        problemDetail.setTitle("Access Denied");
        problemDetail.setType(URI.create(BASE_ERROR_URL + "access-denied"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        String traceId = UUID.randomUUID().toString();
        log.error("Unhandled exception [traceId={}]: ", traceId, ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred. Please contact support with trace ID: " + traceId
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create(BASE_ERROR_URL + "internal-error"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}
