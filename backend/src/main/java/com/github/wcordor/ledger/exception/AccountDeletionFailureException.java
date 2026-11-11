package com.github.wcordor.ledger.exception;

public class AccountDeletionFailureException extends RuntimeException {

    public AccountDeletionFailureException(Long accountId) {
		super("Account " + accountId + " can't be deleted because it does not have an empty balance.");
	}
}
