package com.abhishek.fintech.wallet.exception;

public class InvalidWalletStateException extends RuntimeException {
    public InvalidWalletStateException(String message) {
        super(message);
    }
}
