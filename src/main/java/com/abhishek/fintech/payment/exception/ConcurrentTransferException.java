package com.abhishek.fintech.payment.exception;

public class ConcurrentTransferException extends RuntimeException {
    public ConcurrentTransferException(String message) {
        super(message);
    }
}
