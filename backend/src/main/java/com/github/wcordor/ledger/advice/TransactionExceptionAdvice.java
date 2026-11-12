package com.github.wcordor.ledger.advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.wcordor.ledger.exception.InsufficientFundsException;
import com.github.wcordor.ledger.exception.InvalidAccountIdException;
import com.github.wcordor.ledger.exception.InvalidTransferException;
import com.github.wcordor.ledger.exception.TransactionNotFoundException;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class TransactionExceptionAdvice {

    @ExceptionHandler(TransactionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String transactionNotFoundHandler(TransactionNotFoundException e) {
        return e.getMessage();
    }

    @ExceptionHandler(InvalidTransferException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String invalidTransferHandler(InvalidTransferException e) {
        return e.getMessage();
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public String insufficientFundsHandler(InsufficientFundsException e) {
        return e.getMessage();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String entityNotFoundHandler(EntityNotFoundException e) {
        return e.getMessage();
    }

    @ExceptionHandler(InvalidAccountIdException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String invalidAccountIdHandler(InvalidAccountIdException e) {
        return e.getMessage();
    }
}
