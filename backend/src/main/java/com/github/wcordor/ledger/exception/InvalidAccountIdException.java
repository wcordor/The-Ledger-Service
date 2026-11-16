package com.github.wcordor.ledger.exception;

public class InvalidAccountIdException extends RuntimeException {
    
    public InvalidAccountIdException(Long accountId) {
        super("The transaction does not belong to account #" + accountId + ".");
    }
}
