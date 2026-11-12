package com.github.wcordor.ledger.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String username) {
		super("Could not find User " + username + ".");
	}

	public UserNotFoundException(Long id) {
		super("Could not find User #" + id + ".");
	}

}
