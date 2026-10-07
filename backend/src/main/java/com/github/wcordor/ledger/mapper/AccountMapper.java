package com.github.wcordor.ledger.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.github.wcordor.ledger.dtos.accountDTO.AccountCreationDTO;
import com.github.wcordor.ledger.dtos.accountDTO.AccountResponseDTO;
import com.github.wcordor.ledger.entity.Account;
import com.github.wcordor.ledger.entity.LedgerUser;
import com.github.wcordor.ledger.entity.Transaction;
import com.github.wcordor.ledger.exception.UserNotFoundException;
import com.github.wcordor.ledger.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Component
public class AccountMapper {

    private final UserRepository userRepository;

    public AccountMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public AccountResponseDTO toDTO(Account account) {
        String name = account.getName();
        BigDecimal balance = account.getBalance();
        String currency = account.getCurrency();

        @SuppressWarnings("null")
        List<String> transactions = account.getTransactions().stream()
            .map(Transaction::getAmountAndCurrency).toList();
        
        Long userId = account.getUserId();
        LedgerUser user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Could not find user " + userId + "."));

        String owner = user.getName();
        Long id = account.getId();

        return new AccountResponseDTO(name, balance, currency, owner, transactions, id);
    }

    public Account toAccount(AccountCreationDTO accountDTO) {

        Long userId = accountDTO.userId();
        LedgerUser user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Could not find user " + userId + "."));

        return new Account(user, accountDTO.name(), accountDTO.initialDeposit(), accountDTO.currency());
    }
}
