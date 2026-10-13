package com.github.wcordor.ledger.exception;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("Username " + username + " has already been taken.");
    }
    
}
