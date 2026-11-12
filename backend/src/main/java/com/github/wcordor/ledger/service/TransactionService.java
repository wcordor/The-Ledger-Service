package com.github.wcordor.ledger.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.wcordor.ledger.dtos.transactionDTO.*;
import com.github.wcordor.ledger.entity.Account;
import com.github.wcordor.ledger.entity.IdempotencyKey;
import com.github.wcordor.ledger.entity.LedgerUser;
import com.github.wcordor.ledger.entity.Transaction;
import com.github.wcordor.ledger.exception.AccountNotFoundException;
import com.github.wcordor.ledger.exception.IdempotencyKeyAlreadyExistsException;
import com.github.wcordor.ledger.exception.InsufficientFundsException;
import com.github.wcordor.ledger.exception.InvalidAccountIdException;
import com.github.wcordor.ledger.exception.InvalidTransferException;
import com.github.wcordor.ledger.exception.InvalidUsernameException;
import com.github.wcordor.ledger.exception.TransactionNotFoundException;
import com.github.wcordor.ledger.exception.UserNotFoundException;
import com.github.wcordor.ledger.mapper.TransactionMapper;
import com.github.wcordor.ledger.repository.AccountRepository;
import com.github.wcordor.ledger.repository.IdempotencyKeyRepository;
import com.github.wcordor.ledger.repository.TransactionRepository;
import com.github.wcordor.ledger.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;


@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final AccountService accountService;
    private final TransactionMapper transactionMapper;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository,
        UserRepository userRepository, IdempotencyKeyRepository idempotencyKeyRepository, AccountService accountService,
        TransactionMapper transactionMapper) {
            
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.accountService = accountService;
        this.transactionMapper = transactionMapper;
    }

    @Retryable(retryFor = { PessimisticLockingFailureException.class }, maxAttempts = 3,
        backoff = @Backoff(delay = 50, maxDelay = 150, multiplier = 2.0))
    @Transactional(rollbackFor = { InsufficientFundsException.class })
    public TransactionResponseDTO moneyTransfer(String idempotencyKey, Long sender_userId, Long senderId,
        TransactionCreationDTO transactionDTO) throws InsufficientFundsException {
            
        IdempotencyKey savedKey = idempotencyKeyRepository.findByKey(idempotencyKey).orElse(null);

        if (savedKey != null) {
            if (savedKey.getExpiryDate().isBefore(LocalDateTime.now())) {
                idempotencyKeyRepository.delete(savedKey);
            } else {
                throw new IdempotencyKeyAlreadyExistsException();
            }
        }

        if (senderId == transactionDTO.receiverId()) {
            throw new InvalidTransferException();
        }
        
        Account sender = accountRepository.findWithLockingByIdAndUser_Id(senderId, sender_userId)
        .orElseThrow(() -> new AccountNotFoundException(senderId));

        Long receiverId = transactionDTO.receiverId();

        Account receiver = accountRepository.findWithLockingById(receiverId)
            .orElseThrow(() -> new EntityNotFoundException("Account " + receiverId + " not found"));
        
        BigDecimal amount = transactionDTO.amount();

        BigDecimal expected_senderBal = sender.getBalance().subtract(amount);
        
        if (expected_senderBal.signum() == -1) {
            throw new InsufficientFundsException();
        }

        sender.debit(amount);
        receiver.credit(amount);

        Transaction transaction = transactionRepository.save(
            new Transaction(receiver, sender, amount, transactionDTO.currency())
        );

        IdempotencyKey newKey = new IdempotencyKey(idempotencyKey, LocalDateTime.now().plusHours(24));
        idempotencyKeyRepository.save(newKey);

        return transactionMapper.toDTO(transaction);
    }

    @SuppressWarnings("null")
    public List<String> getTransactions(String username, Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        Long userId = account.getUserId();
        LedgerUser user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        
        if (!username.equals(user.getUsername())) {
            throw new InvalidUsernameException();
        }
        
        return account.getTransactions().stream().map(Transaction::getAmountAndCurrency).toList();
    }

    public TransactionResponseDTO getTransaction(String username, Long accountId, Long transactionId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        Long userId = account.getUserId();
        LedgerUser user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        if (!username.equals(user.getUsername())) {
            throw new InvalidUsernameException();
        }

        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (transaction.getSenderId() != accountId && transaction.getReceiverId() != accountId) {
            throw new InvalidAccountIdException(accountId);
        }

        return transactionMapper.toDTO(transaction);
    }
    
}
