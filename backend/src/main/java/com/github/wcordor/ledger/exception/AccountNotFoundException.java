package com.github.wcordor.ledger.exception;

public class AccountNotFoundException extends RuntimeException {

	public AccountNotFoundException(Long accountId) {
		super("Account #" + accountId + " could not be found.");
	}

}
