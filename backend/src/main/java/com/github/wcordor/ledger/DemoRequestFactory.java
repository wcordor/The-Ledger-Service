package com.github.wcordor.ledger;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.github.wcordor.ledger.dtos.accountDTO.AccountCreationDTO;
import com.github.wcordor.ledger.dtos.accountDTO.AccountResponseDTO;
import com.github.wcordor.ledger.dtos.transactionDTO.*;
import com.github.wcordor.ledger.dtos.userDTO.UserCreationDTO;
import com.github.wcordor.ledger.dtos.userDTO.UserResponseDTO;
import com.github.wcordor.ledger.entity.Account;
import com.github.wcordor.ledger.entity.LedgerUser;
import com.github.wcordor.ledger.entity.Transaction;
import com.github.wcordor.ledger.exception.AccountNotFoundException;
import com.github.wcordor.ledger.exception.TransactionNotFoundException;
import com.github.wcordor.ledger.exception.UserNotFoundException;
import com.github.wcordor.ledger.repository.AccountRepository;
import com.github.wcordor.ledger.repository.TransactionRepository;
import com.github.wcordor.ledger.repository.UserRepository;
import com.github.wcordor.ledger.service.*;

@Component
public class DemoRequestFactory {

    private final UserService userService;
    private final AccountService accountService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    public DemoRequestFactory(UserService userService, AccountService accountService, UserRepository userRepository,
        AccountRepository accountRepository, TransactionRepository transactionRepository, TransactionService transactionService) {

        this.userService = userService;
        this.accountService = accountService;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
    }
    
    public UserResponseDTO createDemoUser(String firstName, String lastName, String username, String password, Role role) {

        return userService.createUser(UUID.randomUUID().toString(),
			new UserCreationDTO(firstName, lastName, username, password, role));
    }

    public AccountResponseDTO createDemoAccount(/*Long userId*/String username, String name, BigDecimal initialDeposit, String currency) {

        LedgerUser user = userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
        
        return accountService.createAccount(
            UUID.randomUUID().toString(), username, new AccountCreationDTO(name, initialDeposit, currency, user.getId())
        );    
    }

    public LedgerUser getDemoUser(/*Long id*/String username) {
        
        return userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
    }

    public Account getDemoAccount(Long accountId, Long userId) {

        return accountRepository.findByIdAndUser_Id(accountId, userId).orElseThrow(() -> new AccountNotFoundException(accountId, userId));
    }

    public Transaction getDemoTransaction(Long transactionId, Long accountId, Long userId) {

        getDemoAccount(accountId, userId);

        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId, accountId));

        if (transaction.getSenderId() != accountId && transaction.getReceiverId() != accountId) {
            throw new TransactionNotFoundException(transactionId, accountId);
        }

        return transactionRepository.findById(transactionId).orElseThrow(() -> new TransactionNotFoundException(transactionId, accountId));
    }

    public TransactionResponseDTO demoMoneyTransfer(Long userId, Long senderId, Long receiverId, BigDecimal amount, String currency) {

        return transactionService.moneyTransfer(UUID.randomUUID().toString(), userId,
            senderId, new TransactionCreationDTO(receiverId, amount, currency));
    }
}
