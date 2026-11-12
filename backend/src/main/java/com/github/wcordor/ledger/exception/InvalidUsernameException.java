package com.github.wcordor.ledger.exception;

public class InvalidUsernameException extends RuntimeException {

    public InvalidUsernameException() {
        super("Username does not match username of Account Owner.");
    }
}
