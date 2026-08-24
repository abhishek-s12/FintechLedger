package com.abhishek.fintech.payment.exception;

public class SelfTransferNotAllowedException extends RuntimeException {
    public SelfTransferNotAllowedException(String message) {
        super(message);
    }
}
