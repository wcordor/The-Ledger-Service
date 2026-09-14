package com.github.wcordor.ledger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import com.github.wcordor.Role;
import com.github.wcordor.ledger.dtos.accountDTO.AccountResponseDTO;
import com.github.wcordor.ledger.dtos.transactionDTO.TransactionResponseDTO;
import com.github.wcordor.ledger.dtos.userDTO.UserResponseDTO;
import com.github.wcordor.ledger.exception.InsufficientFundsException;
import com.github.wcordor.ledger.repository.AccountRepository;
import com.github.wcordor.ledger.repository.UserRepository;
import com.github.wcordor.ledger.service.AccountService;
import com.github.wcordor.ledger.service.TransactionService;
import com.github.wcordor.ledger.service.UserService;

@Configuration
@EnableRetry
public class Demo {
    
    private static final Logger logger = LoggerFactory.getLogger(LedgerApplication.class);
    
    @Bean
	public CommandLineRunner demoRunner(UserRepository userRepository, AccountRepository accountRepository,
		TransactionService transactionService, AccountService accountService, UserService userService, DemoRequestFactory requestFactory) {

		return (args) -> {

			UserResponseDTO userDTO1 = requestFactory.createDemoUser("John", "Smith", "jsmith",
				"$2a$12$OcPK4aV9I39qU9IJTkbhZukUdA4N1O7CXV0YC75bKON/KiYWpNXQC", Role.USER); // password: 1

			Long user1_id = userDTO1.id();

			AccountResponseDTO accountDTO1 = requestFactory.createDemoAccount(user1_id,
				"Savings", new BigDecimal("5000.00"), "GBP");

			AccountResponseDTO accountDTO2 = requestFactory.createDemoAccount(user1_id,
				"Checking", new BigDecimal("1000.00"), "GBP");

			UserResponseDTO userDTO2 = requestFactory.createDemoUser("Bernard", "Jones", "bjones",
				"$2a$12$fip3pRbbenWjFqjv1nhfHu4INrG83eN.bLyvQUADCKRXbqdXGzGS.", Role.USER); // password: 2

			Long user2_id = userDTO2.id();

			AccountResponseDTO accountDTO3 = requestFactory.createDemoAccount(user2_id,
				"Investment", new BigDecimal("15000.00"), "USD");

			AccountResponseDTO accountDTO4 = requestFactory.createDemoAccount(user2_id, 
				"Savings", new BigDecimal("7000.00"), "USD");

			AccountResponseDTO accountDTO5 = requestFactory.createDemoAccount(user2_id,
				"Checking", new BigDecimal("3000.00"), "USD");

			UserResponseDTO userDTO3 = requestFactory.createDemoUser("Deborah", "Adams", "dadams",
				"$2a$12$SJ1QAz30MHb5YlYJtYNfzeLrZwFP9zy0IBrZ9NBhw5QKO.yNZvGgi", Role.USER); // password: 3

			Long user3_id = userDTO3.id();

			AccountResponseDTO accountDTO6 = requestFactory.createDemoAccount(user3_id,
				"Savings", new BigDecimal("3000.00"), "USD");

			AccountResponseDTO accountDTO7 = requestFactory.createDemoAccount(user3_id,
				"Checking", new BigDecimal("1000.00"), "USD");

			UserResponseDTO userDTO4 = requestFactory.createDemoUser("Mary", "Johnson", "mjohnson",
				"$2a$12$0n/kVnACkkrKzYIfYM0wN.a5iErdHAOmi4S51Wg.HxG5v4.PqVyi2", Role.USER); // password: 4

			Long user4_id = userDTO4.id();

			AccountResponseDTO accountDTO8 = requestFactory.createDemoAccount(user4_id,
				"Savings", new BigDecimal("5500.00"), "USD");

			AccountResponseDTO accountDTO9 = requestFactory.createDemoAccount(user4_id,
				"Checking", new BigDecimal("1500.00"), "USD");

			UserResponseDTO admin = requestFactory.createDemoUser("Admin", "User", "admin",
				"$2a$12$Bvn4neIXr3Sf68k7zLfMAO6844POfyDVwhcf4fdRgPd9qpAI/P6Ju", Role.ADMIN); // password: 99

			logger.info("");
			logger.info("List of Preloaded Users:");
			logger.info("------------------------");
			userRepository.findAll().forEach(user -> {
				logger.info(user.toString());
			});
			logger.info("");

			logger.info("List of Preloaded Accounts:");
			logger.info("---------------------------");
			accountRepository.findAll().forEach(acc -> {
				logger.info(acc.toString());
			});
			logger.info("");

			logger.info("Accounts that use USD:");
			logger.info("----------------------");
			accountRepository.findByCurrency("USD").forEach(usd -> {
				logger.info(usd.toString());
			});
			logger.info("");

			logger.info("Accounts that use GBP:");
			logger.info("----------------------");
			accountRepository.findByCurrency("GBP").forEach(gbp -> {
				logger.info(gbp.toString());
			});
			logger.info("");

			Long account5_id = accountDTO5.id();
			Long account7_id = accountDTO7.id();

			accountDTO5 = accountService.getAccount(account5_id, user2_id);
			accountDTO7 = accountService.getAccount(account7_id, user3_id);
			BigDecimal account5_bal = accountDTO5.balance();
			BigDecimal account7_bal = accountDTO7.balance();
			String account5_currency = accountDTO5.currency();
			String account7_currency = accountDTO7.currency();
			
			logger.info("Account " + account5_id + " (B. Jones) transfer 500 USD to Account " + account7_id + " (D. Adams)"); 
			logger.info("-------------------------------------------------------------");
			logger.info(String.format("Balances before transfer: Account %d - %,.2f %s, Account %d - %,.2f %s",
				account5_id, account5_bal, account5_currency, account7_id, account7_bal, account7_currency));

			TransactionResponseDTO transactionDTO = requestFactory.demoMoneyTransfer(user2_id,
				account5_id, account7_id, new BigDecimal("500.00"), "USD");

			accountDTO5 = accountService.getAccount(account5_id, user2_id);
			accountDTO7 = accountService.getAccount(account7_id, user3_id);
			account5_bal = accountDTO5.balance();
			account7_bal = accountDTO7.balance();

			logger.info(String.format("Balances after transfer: Account %d - %,.2f %s, Account %d - %,.2f %s",
				account5_id, account5_bal, account5_currency, account7_id, account7_bal, account7_currency));
			if (account5_bal.compareTo(new BigDecimal("2500.00")) == 0
				&& account7_bal.compareTo(new BigDecimal("1500.00")) == 0) {
				logger.info("***********************");
				logger.info("Transaction successful.");
				logger.info("***********************");
				logger.info("");
			}

			logger.info("Transaction Info:");
			logger.info(transactionDTO.toString());

			try {
				logger.info("Account " + account7_id + " (D. Adams) transfer 2,000 USD to Account " + account5_id + " (B. Jones)"); 
				logger.info("---------------------------------------------------------------");	
				logger.info(String.format("Balances before transfer: Account %d - %,.2f %s, Account %d - %,.2f %s",
					account7_id, account7_bal, account7_currency, account5_id, account5_bal, account5_currency));

				requestFactory.demoMoneyTransfer(user3_id, account7_id, account5_id, new BigDecimal("2000.00"), "USD");

				logger.info("");
			} catch (InsufficientFundsException e) {
				logger.info("");
				logger.error("ERROR: " + e.getMessage());
				logger.info("");
			}

			accountDTO5 = accountService.getAccount(account5_id, user2_id);
			accountDTO7 = accountService.getAccount(account7_id, user3_id);
			BigDecimal account5_bal_rolledBack = accountDTO5.balance();
			BigDecimal account7_bal_rolledBack = accountDTO7.balance();

			logger.info(String.format("Balances after transfer: Account %d - %,.2f %s, Account %d - %,.2f %s",
				account7_id, account7_bal, account7_currency, account5_id, account5_bal, account5_currency));

			if (account5_bal_rolledBack.compareTo(account5_bal) == 0
			&& account7_bal_rolledBack.compareTo(account7_bal) == 0) {
				logger.info("********************");
				logger.info("Rollback successful.");
				logger.info("********************");
				logger.info("");
			}

			Long account9_id = accountDTO9.id();
			accountDTO9 = accountService.getAccount(account9_id, user4_id);
			BigDecimal account9_bal = accountDTO9.balance();
			String account9_currency = accountDTO9.currency();
			
			logger.info("40 simultaneous transactions from Account " + account5_id + " (B. Jones) and Account " + account7_id);
			logger.info("(D. Adams), to Account " + account9_id + " (M. Johnson)");
			logger.info("--------------------------------------------------------------------");
			logger.info("Balances before transfers:");
			logger.info("");
			logger.info(String.format("Account %d - %,.2f %s", account9_id, account9_bal, account9_currency));
			logger.info(String.format("Account %d - %,.2f %s", account5_id, account5_bal, account5_currency));
			logger.info(String.format("Account %d - %,.2f %s", account7_id, account7_bal, account7_currency));
			logger.info("");

			List<CompletableFuture<Void>> futures = new ArrayList<>();

			for (int i = 0; i < 20; i++) {
				CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
					try {
						requestFactory.demoMoneyTransfer(user2_id, account5_id, account9_id, new BigDecimal("50.00"), "USD");
					} catch (InsufficientFundsException e) {
						logger.info("");
						logger.error("ERROR: " + e.getMessage());
						logger.info("");
					}
				});
				futures.add(future1);

				CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
					try {
						requestFactory.demoMoneyTransfer(user3_id, account7_id, account9_id, new BigDecimal("25.00"), "USD");
					} catch (InsufficientFundsException e) {
						logger.info("");
						logger.error("ERROR: " + e.getMessage());
						logger.info("");
					}
				});
				futures.add(future2);

			}

			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
			accountRepository.flush();

			accountDTO5 = accountService.getAccount(account5_id, user2_id);
			accountDTO7 = accountService.getAccount(account7_id, user3_id);
			accountDTO9 = accountService.getAccount(account9_id, user4_id);

			account9_bal = accountDTO9.balance();
			account5_bal = accountDTO5.balance();
			account7_bal = accountDTO7.balance();

			logger.info("Balances after transfers:");
			logger.info("");
			logger.info(String.format("Account %d - %,.2f %s", account9_id, account9_bal, account9_currency));
			logger.info(String.format("Account %d - %,.2f %s", account5_id, account5_bal, account5_currency));
			logger.info(String.format("Account %d - %,.2f %s", account7_id, account7_bal, account7_currency));
			if (account9_bal.compareTo(new BigDecimal("3000.00")) == 0
			&& account5_bal.compareTo(new BigDecimal("1500.00")) == 0
			&& account7_bal.compareTo(new BigDecimal("1000.00")) == 0) {
				logger.info("***********************************");
				logger.info("Concurrent transactions successful.");
				logger.info("***********************************");
				logger.info("Account " + account9_id + " total transactions: " + accountDTO9.transactions().size());
				logger.info("Account " + account5_id + " total transactions: " + accountDTO5.transactions().size());
				logger.info("Account " + account7_id + " total transactions: " + accountDTO7.transactions().size());
				
			}
		};
	}
}
