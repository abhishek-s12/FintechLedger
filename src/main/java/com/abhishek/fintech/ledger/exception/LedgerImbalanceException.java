package com.abhishek.fintech.ledger.exception;

public class LedgerImbalanceException extends RuntimeException {
    public LedgerImbalanceException(String message) {
        super(message);
    }
}
