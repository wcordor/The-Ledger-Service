package com.github.wcordor.ledger.advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.wcordor.ledger.exception.InvalidUserIdException;
import com.github.wcordor.ledger.exception.UserDeletionFailureException;
import com.github.wcordor.ledger.exception.UserNotFoundException;
import com.github.wcordor.ledger.exception.UsernameAlreadyExistsException;

@RestControllerAdvice
public class UserExceptionAdvice {
    
    @ExceptionHandler(UserNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String userNotFoundHandler(UserNotFoundException e) {
		return e.getMessage();
	}

	@ExceptionHandler(UserDeletionFailureException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public String userDeletionFailureHandler(UserDeletionFailureException e) {
		return e.getMessage();
	}

	@ExceptionHandler(InvalidUserIdException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public String InvalidUserIdHandler(InvalidUserIdException e) {
		return e.getMessage();
	}

	@ExceptionHandler(UsernameAlreadyExistsException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public String UsernameAlreadyExistsHandler(UsernameAlreadyExistsException e) {
		return e.getMessage();
	}

}
