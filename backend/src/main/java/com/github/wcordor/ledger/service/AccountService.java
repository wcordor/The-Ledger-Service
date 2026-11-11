package com.github.wcordor.ledger.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.wcordor.ledger.dtos.accountDTO.*;
import com.github.wcordor.ledger.entity.Account;
import com.github.wcordor.ledger.entity.IdempotencyKey;
import com.github.wcordor.ledger.entity.LedgerUser;
import com.github.wcordor.ledger.exception.AccountDeletionFailureException;
import com.github.wcordor.ledger.exception.AccountNotFoundException;
import com.github.wcordor.ledger.exception.IdempotencyKeyAlreadyExistsException;
import com.github.wcordor.ledger.exception.NullPatchFieldException;
import com.github.wcordor.ledger.exception.UserNotFoundException;
import com.github.wcordor.ledger.mapper.AccountMapper;
import com.github.wcordor.ledger.repository.AccountRepository;
import com.github.wcordor.ledger.repository.IdempotencyKeyRepository;
import com.github.wcordor.ledger.repository.UserRepository;


@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final AccountMapper accountMapper;
    private final UserService userService;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository,
        IdempotencyKeyRepository idempotencyKeyRepository, AccountMapper accountMapper, UserService userService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.accountMapper = accountMapper;
        this.userService = userService;
    }

    public AccountResponseDTO createAccount(String idempotencyKey, String username, AccountCreationDTO accountDTO) {
        IdempotencyKey savedKey = idempotencyKeyRepository.findByKey(idempotencyKey).orElse(null);

        if (savedKey != null) {
            if (savedKey.getExpiryDate().isBefore(LocalDateTime.now())) {
                idempotencyKeyRepository.delete(savedKey);
            } else {
                throw new IdempotencyKeyAlreadyExistsException();
            }
        }

        Account account = accountRepository.save(accountMapper.toAccount(username, accountDTO));

        IdempotencyKey newKey = new IdempotencyKey(idempotencyKey, LocalDateTime.now().plusHours(24));
        idempotencyKeyRepository.save(newKey);
        
        return accountMapper.toDTO(account);
    }
    
    @SuppressWarnings("null")
    public List<String> getAccounts(String username) {

        LedgerUser user = userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
        Long id = user.getId();

        return accountRepository.findByUser_Id(id).stream().map(Account::getInfo).toList();
    }

    public AccountResponseDTO getAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        return accountMapper.toDTO(account);
    }

    @Transactional
    public AccountResponseDTO changeName(String idempotencyKey, Long accountId, AccountPatchDTO accountDTO) {
        IdempotencyKey savedKey = idempotencyKeyRepository.findByKey(idempotencyKey).orElse(null);

        if (savedKey != null) {
            if (savedKey.getExpiryDate().isBefore(LocalDateTime.now())) {
                idempotencyKeyRepository.delete(savedKey);
            } else {
                throw new IdempotencyKeyAlreadyExistsException();
            }
        }

        Account account = accountRepository.findWithLockingById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (accountDTO.getName().isPresent()) {
            account.setName(accountDTO.getName().get());

            if (accountDTO.getName().get() == null || accountDTO.getName().get().isBlank()) {
                throw new NullPatchFieldException();
            };
        }

        IdempotencyKey newKey = new IdempotencyKey(idempotencyKey, LocalDateTime.now().plusHours(24));
        idempotencyKeyRepository.save(newKey);
        
        return accountMapper.toDTO(account);        
    }
    
    public void deleteAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
            
        if (account.getBalance().compareTo(new BigDecimal("0.00")) != 0) {
            throw new AccountDeletionFailureException(accountId);
        }

        accountRepository.deleteById(accountId);
    }

}
