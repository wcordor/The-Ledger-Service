package com.github.wcordor.ledger.advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.wcordor.ledger.exception.AccountDeletionFailureException;
import com.github.wcordor.ledger.exception.AccountNotFoundException;
import com.github.wcordor.ledger.exception.InvalidUsernameException;

@RestControllerAdvice
public class AccountExceptionAdvice {

  @ExceptionHandler(AccountNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String accountNotFoundHandler(AccountNotFoundException e) {
    return e.getMessage();
  }

  @ExceptionHandler(AccountDeletionFailureException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public String accountNotDeletedHandler(AccountDeletionFailureException e) {
    return e.getMessage();
  }

  @ExceptionHandler(InvalidUsernameException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public String invalidUsernameHandler(InvalidUsernameException e) {
    return e.getMessage();
  }

}
