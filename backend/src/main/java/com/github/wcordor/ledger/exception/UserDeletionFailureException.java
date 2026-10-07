package com.github.wcordor.ledger.exception;

public class UserDeletionFailureException extends RuntimeException {

    public UserDeletionFailureException() {
		super("Users with active accounts cannot be deleted.");
	}
}
