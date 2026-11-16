package com.github.wcordor.ledger.exception;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(Long transactionId) {
        super("Transaction #" + transactionId + " was not found.");
    }

}
