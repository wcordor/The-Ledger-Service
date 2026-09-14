package com.github.wcordor.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.github.wcordor.Role;
import com.github.wcordor.ledger.dtos.accountDTO.*;
import com.github.wcordor.ledger.dtos.transactionDTO.*;
import com.github.wcordor.ledger.dtos.userDTO.*;
import com.github.wcordor.ledger.entity.Account;
import com.github.wcordor.ledger.entity.Transaction;
import com.github.wcordor.ledger.entity.LedgerUser;
import com.github.wcordor.ledger.exception.*;
import com.github.wcordor.ledger.mapper.AccountMapper;
import com.github.wcordor.ledger.mapper.TransactionMapper;
import com.github.wcordor.ledger.mapper.UserMapper;
import com.github.wcordor.ledger.repository.*;
import com.github.wcordor.ledger.service.*;

import jakarta.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest
class LedgerApplicationTests {

	private static final Logger logger = LoggerFactory.getLogger(LedgerApplicationTests.class);

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TransactionService transactionService;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private IdempotencyKeyRepository idempotencyKeyRepository;

	@Autowired
	private AccountService accountService;

	@Autowired
	private UserService userService;

    @Autowired
    DemoRequestFactory requestFactory;

	@Autowired
	AccountMapper accountMapper;

	@Autowired
	UserMapper userMapper;

	@Autowired
	TransactionMapper transactionMapper;

    private AccountResponseDTO accountDTO;
    private Account account;
    private Long account_id;
    private AccountResponseDTO accountDTO2;
    private Account account2;
    private Long account2_id;

    private UserResponseDTO userDTO;
    private LedgerUser user;
    private Long user_id;
    private UserResponseDTO userDTO2;
    private LedgerUser user2;
    private Long user2_id;

	@BeforeEach
	void setUp() {

		accountRepository.deleteAll();
		userRepository.deleteAll();
		transactionRepository.deleteAll();
		idempotencyKeyRepository.deleteAll();

        userDTO = requestFactory.createDemoUser("Account", "Owner I", "owner1", "p", Role.USER);
        user_id = userDTO.id();

        accountDTO = requestFactory.createDemoAccount(user_id, "Account I",
            new BigDecimal("1000.00"), "USD");

        account_id = accountDTO.id();

        userDTO = userService.getUser(user_id);
       
        userDTO2 = requestFactory.createDemoUser("Account", "Owner II","owner2", "a", Role.USER);
        user2_id = userDTO2.id();

        accountDTO2 = requestFactory.createDemoAccount(user2_id, "Account II", new BigDecimal("200.00"), "USD");
        account2_id = accountDTO2.id();

        userDTO2 = userService.getUser(user2_id);

        account = requestFactory.getDemoAccount(account_id, user_id);
        account2 = requestFactory.getDemoAccount(account2_id, user2_id);
        user = requestFactory.getDemoUser(user_id);
        user2 = requestFactory.getDemoUser(user2_id);

	}

	@Test
	void testAccountFunctions() {

		assertNotNull(account_id);
		assertEquals("Account I", account.getName());
		assertEquals(new BigDecimal("1000.00"), account.getBalance());
		assertEquals("USD", account.getCurrency());
		assertEquals(user_id, account.getUserId());

		account.setName("Checking");
		assertEquals("Checking", account.getName());

		account.setCurrency("GBP");
		assertEquals("GBP", account.getCurrency());
		account.setUser(user2);
		assertNotEquals(user2_id, account.getUserId());
		assertEquals(user_id, account.getUserId());

		account.debit(new BigDecimal("100.00"));
		assertEquals(new BigDecimal("900.00"), account.getBalance());

		account.credit(new BigDecimal("200.00"));
		assertEquals(new BigDecimal("1100.00"), account.getBalance());

	}

	@Test
	void checkAccountDTOFields() {

		assertEquals(account_id, accountDTO.id());
		assertEquals("Account I", accountDTO.name());
		assertEquals(new BigDecimal("1000.00"), accountDTO.balance());
		assertEquals("USD", accountDTO.currency());
		assertEquals("Account Owner I", accountDTO.userName());
		assertTrue(accountDTO.transactions().isEmpty());

	}

	@Test
	void testAccountRepositoryFunctions() {

		assertEquals(1, accountRepository.findByName("Account I").size());
		assertTrue(accountRepository.findByName("Account I").contains(account));
		
		assertEquals(account,
			accountRepository.findById(account_id).orElseThrow(() -> new EntityNotFoundException("Account not found")));
		assertEquals(1, accountRepository.findByUserLastName("Owner I").size());
		assertTrue(accountRepository.findByUserLastName("Owner I").contains(account));

		List<Account> usd = accountRepository.findByCurrency("USD");
		assertEquals(2, usd.size());
		assertTrue(usd.contains(account) && usd.contains(account2));

		List<Account> userAccounts = accountRepository.findByUser_Id(user_id);
		assertEquals(1, userAccounts.size());
		assertTrue(userAccounts.contains(account));

		Account actual = accountRepository.findByIdAndUser_Id(account_id, user_id)
			.orElseThrow(() -> new AccountNotFoundException(account_id, user_id));

		assertEquals(account, actual);
			
	}

	@Test
	void testAccountServiceFunctions() {

		AccountCreationDTO creationDTO = new AccountCreationDTO("Account III", new BigDecimal("300.00"),
			"USD", user_id);
			
		AccountResponseDTO accountDTO3 = accountService.createAccount("Key-Test", user_id, creationDTO);

		Long account3_id = accountDTO3.id();
		Account account3 = requestFactory.getDemoAccount(account3_id, user_id);

		List<String> accountList = accountService.getAccounts(user_id);
		assertEquals(2, accountList.size());
		assertTrue(accountList.contains(account.getInfo()) && accountList.contains(account3.getInfo()));

		AccountResponseDTO getAccount = accountService.getAccount(account3_id, user_id);
		assertEquals(accountDTO3, getAccount);

		AccountPatchDTO patchDTO = new AccountPatchDTO(JsonNullable.of("Savings"));
		accountDTO3 = accountService.changeName(UUID.randomUUID().toString(),
			account3_id, user_id, patchDTO);

		assertEquals("Savings", accountDTO3.name());

		AccountPatchDTO nullPatch = new AccountPatchDTO(JsonNullable.of(null));
		assertThrows(NullPatchFieldException.class, () -> {
			accountService.changeName(UUID.randomUUID().toString(), account3_id, user_id, nullPatch);
		});

		AccountPatchDTO blankPatch = new AccountPatchDTO(JsonNullable.of(" "));
		assertThrows(NullPatchFieldException.class, () -> {
			accountService.changeName(UUID.randomUUID().toString(), account3_id, user_id, blankPatch);
		});

		assertThrows(AccountDeletionFailureException.class, () -> {
			accountService.deleteAccount(account3_id, user_id);
		});

		TransactionCreationDTO transferDTO = new TransactionCreationDTO(account_id, new BigDecimal("300.00"), "USD");
		transactionService.moneyTransfer(UUID.randomUUID().toString(), user_id, account3_id, transferDTO);

		accountService.deleteAccount(account3_id, user_id);

		accountList = accountService.getAccounts(user_id);	
		account = requestFactory.getDemoAccount(account_id, user_id);

		assertTrue(accountList.size() == 1 && accountList.contains(account.getInfo()));

		assertThrows(AccountNotFoundException.class, () -> {
			accountService.getAccount(account3_id, user_id);
		});

		AccountCreationDTO mismatch = new AccountCreationDTO("user_id mismatch", new BigDecimal("0.00"), "USD", user_id);
		assertThrows(InvalidUserIdException.class, () -> {
			accountService.createAccount(UUID.randomUUID().toString(), user2_id, mismatch);
		});

		assertThrows(UserNotFoundException.class, () -> {
			accountService.createAccount(UUID.randomUUID().toString(), 99L, creationDTO);
		});
	}

	@Test
	void testUserFunctions() {

		assertNotNull(user_id);
		assertEquals("Account", user.getFirstName());
		assertEquals("Owner I", user.getLastName());
		assertNotNull(user2_id);
		assertEquals("Account", user2.getFirstName());
		assertEquals("Owner II", user2.getLastName());

		assertEquals(1, user.getAccounts().size());
		assertTrue(user.getAccounts().contains(account));
		
		user.setFirstName("User");
		user.setLastName("1");
		String name = user.getFirstName() + " " + user.getLastName();
		assertEquals("User 1", name);
		user.addAccount(account2);
		List<Account> userAccs = user.getAccounts();

		assertFalse(2 == userAccs.size() && userAccs.contains(account2));

		AccountResponseDTO account3DTO = requestFactory.createDemoAccount(user_id, "Account III", new BigDecimal("5000.00"), "USD");
		Account account3 = requestFactory.getDemoAccount(account3DTO.id(), user_id);
		user = requestFactory.getDemoUser(user_id);
		userAccs = user.getAccounts();

		assertTrue(2 == userAccs.size());
		assertTrue(userAccs.contains(account) && userAccs.contains(account3));
		
	}

	@Test
	void checkUserDTOFields() {

		assertEquals(user_id, userDTO.id());
		assertEquals("Account", userDTO.firstName());
		assertEquals("Owner I", userDTO.lastName());
		assertTrue(userDTO.accounts().contains(account.getInfo()));
		assertEquals(1, userDTO.accounts().size());

	}

	@Test
	void testUserRepositoryFunctions() {

		assertEquals(1, userRepository.findByLastName("Owner I").size());
		assertEquals(user, userRepository.findById(user_id)
			.orElseThrow(() -> new UserNotFoundException(user_id)));
		assertEquals(1, userRepository.findByLastName("Owner II").size());
		assertEquals(user2, userRepository.findById(user2_id)
			.orElseThrow(() -> new UserNotFoundException(user2_id)));
	}

	@Test
	void testUserServiceFunctions() {

		UserCreationDTO creationDTO = new UserCreationDTO("Owner", "of Account I","owner2", "t", Role.USER);
		userDTO = userService.replaceUser(user_id, creationDTO);
		assertEquals("Owner", userDTO.firstName());
		assertEquals("of Account I", userDTO.lastName());
		assertThrows(UserDeletionFailureException.class, () -> {
			userService.deleteUser(user_id);
		});

		UserResponseDTO delete = userService.createUser(UUID.randomUUID().toString(), creationDTO);

		List<String> userList = userService.getAll();
		assertEquals(3, userList.size());

		userService.deleteUser(delete.id());

		assertThrows(UserNotFoundException.class, () -> {
			userService.getUser(delete.id());
		});

		userList = userService.getAll();
		assertEquals(2, userList.size());

		UserPatchDTO patchDTO = new UserPatchDTO(JsonNullable.undefined(), JsonNullable.of("PATCH"));
		userDTO = userService.updateUser(UUID.randomUUID().toString(), user_id, patchDTO);

		assertEquals("Owner", userDTO.firstName());
		assertEquals("PATCH", userDTO.lastName());

		UserPatchDTO patchDTO2 = new UserPatchDTO(JsonNullable.of("User"), JsonNullable.undefined());
		userDTO = userService.updateUser(UUID.randomUUID().toString(), user_id, patchDTO2);

		assertEquals("User", userDTO.firstName());
		assertEquals("PATCH", userDTO.lastName());

		UserPatchDTO nullPatch = new UserPatchDTO(JsonNullable.of(null), JsonNullable.undefined());
		assertThrows(NullPatchFieldException.class, () -> {
			userService.updateUser(UUID.randomUUID().toString(), user_id, nullPatch);
		});

		UserPatchDTO nullPatch2 = new UserPatchDTO(JsonNullable.of(" "), JsonNullable.undefined());
		assertThrows(NullPatchFieldException.class, () -> {
			userService.updateUser(UUID.randomUUID().toString(), user_id, nullPatch2);
		});

		UserPatchDTO nullPatch3 = new UserPatchDTO(JsonNullable.undefined(), JsonNullable.of(" "));
		assertThrows(NullPatchFieldException.class, () -> {
			userService.updateUser(UUID.randomUUID().toString(), user_id, nullPatch3);
		});

	}

	@Test
	void testMoneyTransfers() {

		// acc balance: $1,000, acc2 balance: $200
		TransactionCreationDTO creationDTO = new TransactionCreationDTO(account2_id, new BigDecimal("400.00"), "USD");
		TransactionResponseDTO transactionDTO = transactionService.moneyTransfer(UUID.randomUUID().toString(), user_id, account_id, creationDTO);

		accountDTO = accountService.getAccount(account_id, user_id);
		accountDTO2 = accountService.getAccount(account2_id, user2_id);

		assertEquals(new BigDecimal("600.00"), accountDTO2.balance());
		assertEquals(new BigDecimal("600.00"), accountDTO.balance());
		
		TransactionCreationDTO insufficient = new TransactionCreationDTO(account2_id, new BigDecimal("4000.00"), "USD");
		assertThrows(InsufficientFundsException.class, () -> {
			transactionService.moneyTransfer(UUID.randomUUID().toString(), user_id, account_id, insufficient);
		});

		try {
			transactionService.moneyTransfer(UUID.randomUUID().toString(), user_id, account_id, insufficient);
		} catch (InsufficientFundsException e) {
			logger.error("ERROR: " + e.getMessage());
		}

        accountDTO = accountService.getAccount(account_id, user_id);
		accountDTO2 = accountService.getAccount(account2_id, user2_id);

		assertEquals(new BigDecimal("600.00"), accountDTO2.balance());
		assertEquals(new BigDecimal("600.00"), accountDTO.balance());

		account = requestFactory.getDemoAccount(account_id, user_id);
        account2 = requestFactory.getDemoAccount(account2_id, user2_id);

        List<Transaction> a_transactions = account.getTransactions();
        List<Transaction> a2_transactions = account2.getTransactions();

		Transaction transaction = requestFactory.getDemoTransaction(transactionDTO.id(), account2_id, user2_id);

        assertEquals(1, a_transactions.size());
        assertEquals(1, a2_transactions.size());
		assertTrue(a_transactions.contains(transaction) && a2_transactions.contains(transaction));

		TransactionCreationDTO sameId = new TransactionCreationDTO(account_id, new BigDecimal("80.00"), "USD");
		assertThrows(InvalidTransferException.class, () -> {
			transactionService.moneyTransfer(UUID.randomUUID().toString(), user_id, account_id, sameId);
		});

		assertThrows(AccountNotFoundException.class, () -> {
			transactionService.moneyTransfer(UUID.randomUUID().toString(), user_id, 99L, creationDTO);
		});

		assertThrows(AccountNotFoundException.class, () -> {
			transactionService.moneyTransfer(UUID.randomUUID().toString(), 99L, account_id, creationDTO);
		});

		TransactionCreationDTO nonexistent = new TransactionCreationDTO(99L, new BigDecimal("80.00"), "USD");
		assertThrows(EntityNotFoundException.class, () -> {
			transactionService.moneyTransfer(UUID.randomUUID().toString(), user_id, account_id, nonexistent);
		});
	
	}

	@Test
	void testTransactionFunctions() {

		TransactionResponseDTO transactionDTO = requestFactory.demoMoneyTransfer(user_id, account_id, account2_id,
			new BigDecimal("400.00"), "USD");
		
		Transaction transaction = requestFactory.getDemoTransaction(transactionDTO.id(), account_id, user_id);

		assertNotNull(transaction.getId());
		assertEquals(account_id, transaction.getSenderId());
		assertEquals(account2_id, transaction.getReceiverId());
		assertEquals(new BigDecimal("400.00"), transaction.getAmount());
		assertEquals("USD", transaction.getCurrency());
		assertNotNull(transaction.getTimestamp());

		List<Long> transactionAccIds = transaction.getAccountIds();
		assertEquals(2, transactionAccIds.size());
		assertTrue(transactionAccIds.contains(account_id) && transactionAccIds.contains(account2_id));
	}

	@Test
	void checkTransactionDTOFields() {

		TransactionResponseDTO transactionDTO = requestFactory.demoMoneyTransfer(user_id, account_id, account2_id,
			new BigDecimal("400.00"), "USD");

		assertNotNull(transactionDTO.id());
		assertEquals(account_id, transactionDTO.senderId());
		assertEquals(account2_id, transactionDTO.receiverId());
		assertEquals(new BigDecimal("400.00"), transactionDTO.amount());
		assertEquals("USD", transactionDTO.currency());
		assertNotNull(transactionDTO.timestamp());

	}

	@Test
	void testTransactionServiceFunctions() {

		TransactionCreationDTO creationDTO = new TransactionCreationDTO(account2_id, new BigDecimal("400.00"), "USD");
		TransactionResponseDTO transactionDTO = transactionService.moneyTransfer(UUID.randomUUID().toString(), user_id, account_id, creationDTO);

		List<String> transactions = transactionService.getTransactions(account_id, user_id);
		assertEquals(1, transactions.size());

		List<String> transactions2 = transactionService.getTransactions(account2_id, user2_id);
		assertEquals(1, transactions2.size());

		Transaction transaction = requestFactory.getDemoTransaction(transactionDTO.id(), account_id, user_id);
		assertTrue(transactions.contains(transaction.getAmountAndCurrency()) && transactions2.contains(transaction.getAmountAndCurrency()));

		TransactionResponseDTO actual = transactionService.getTransaction(transactionDTO.id(), account_id, user_id);
		TransactionResponseDTO actual2 = transactionService.getTransaction(transactionDTO.id(), account2_id, user2_id);
		long diffMillis = Math.abs(Duration.between(transactionDTO.timestamp(), actual.timestamp()).toMillis());
		long diffMillis2 = Math.abs(Duration.between(transactionDTO.timestamp(), actual2.timestamp()).toMillis());

		assertTrue(transactionDTO.id() == actual.id() && transactionDTO.id() == actual2.id());
		assertTrue(transactionDTO.senderId() == actual.senderId() && transactionDTO.senderId() == actual2.senderId());
		assertTrue(transactionDTO.receiverId() == actual.receiverId() && transactionDTO.receiverId() == actual2.receiverId());
		assertTrue(transactionDTO.amount().compareTo(actual.amount()) == 0 && transactionDTO.amount().compareTo(actual2.amount()) == 0);
		assertTrue(transactionDTO.currency().equals(actual.currency()) && transactionDTO.currency().equals(actual2.currency()));
		assertTrue(diffMillis < 1 && diffMillis2 < 1);

		assertThrows(TransactionNotFoundException.class, () -> {
			transactionService.getTransaction(400L, account_id, user_id); 
		});

		AccountResponseDTO accountDTO3 = requestFactory.createDemoAccount(user2_id, "Account III", new BigDecimal("10.00"), "USD");
		assertThrows(TransactionNotFoundException.class, () -> {
			transactionService.getTransaction(transactionDTO.id(), accountDTO3.id(), user2_id); 
		});

		assertThrows(TransactionNotFoundException.class, () -> {
			transactionService.getTransaction(440L, account2_id, user2_id); 
		});

		assertThrows(AccountNotFoundException.class, () -> {
			transactionService.getTransaction(transactionDTO.id(), 99L, user_id);
		});

		assertThrows(AccountNotFoundException.class, () -> {
			transactionService.getTransaction(transactionDTO.id(), account_id, 99L);
		});

		assertThrows(AccountNotFoundException.class, () -> {
			transactionService.getTransaction(transactionDTO.id(), 99L, 99L);
		});

		assertThrows(AccountNotFoundException.class, () -> {
			transactionService.getTransactions(99L, 99L);
		});

		assertThrows(AccountNotFoundException.class, () -> {
			transactionService.getTransactions(account2_id, 99L);
		});

		assertThrows(AccountNotFoundException.class, () -> {
			transactionService.getTransactions(99L, user_id);
		});
	}

	@Test
	void testConcurrencySufficient() {

		List<CompletableFuture<Void>> futures = new ArrayList<>();
		TransactionCreationDTO creationDTO = new TransactionCreationDTO(account2_id, new BigDecimal("10.00"), "USD");

		// generate 50 CompletableFutures
		for (int i = 0; i < 50; i++) {
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				try {
					transactionService.moneyTransfer(UUID.randomUUID().toString(), user_id, account_id, creationDTO);
				} catch (InsufficientFundsException e) {
					logger.error("ERROR: " + e.getMessage());
				}
			});
			futures.add(future);
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		accountRepository.flush();

		accountDTO = accountService.getAccount(account_id, user_id);
		accountDTO2 = accountService.getAccount(account2_id, user2_id);

		assertEquals(new BigDecimal("500.00"), accountDTO.balance());
		assertEquals(new BigDecimal("700.00"), accountDTO2.balance());

	}
	
	@Test
	void testConcurrencyInsufficient() {

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failCount = new AtomicInteger(0);

		TransactionCreationDTO creationDTO = new TransactionCreationDTO(account2_id, new BigDecimal("30.00"), "USD");

		for (int i = 0; i < 50; i++) {
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				try {
					transactionService.moneyTransfer(UUID.randomUUID().toString(), user_id, account_id, creationDTO);
						
					successCount.incrementAndGet();
				} catch (InsufficientFundsException e) {
					logger.error("ERROR: " + e.getMessage());
					failCount.incrementAndGet();
				}
			});
			futures.add(future);
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		accountRepository.flush();

		accountDTO = accountService.getAccount(account_id, user_id);
		accountDTO2 = accountService.getAccount(account2_id, user2_id);

		assertEquals(new BigDecimal("10.00"), accountDTO.balance());
		assertEquals(new BigDecimal("1190.00"), accountDTO2.balance());
		assertEquals(33, successCount.get());
		assertEquals(17, failCount.get());

	}

	@Test
	void testAccountMapperMethods() {

		AccountResponseDTO accountDTO3 = accountMapper.toDTO(account);
		assertEquals(account.getName(), accountDTO3.name());
		assertEquals(account.getBalance(), accountDTO3.balance());
		assertEquals(account.getCurrency(), accountDTO3.currency());
		assertEquals(account.getTransactions().size(), accountDTO3.transactions().size());
		assertEquals(account.getId(), accountDTO3.id());

		Long id = account.getUserId();
        LedgerUser user3 = requestFactory.getDemoUser(id);
		assertEquals(accountDTO3.userName(), user3.getName());

		AccountCreationDTO creationDTO = new AccountCreationDTO("New Account", new BigDecimal("10.00"), "USD", user_id);
		Account account3 = accountMapper.toAccount(creationDTO);

		assertEquals(creationDTO.name(), account3.getName());
		assertEquals(creationDTO.initialDeposit(), account3.getBalance());
		assertEquals(creationDTO.currency(), account3.getCurrency());
		assertEquals(creationDTO.userId(), account3.getUserId());

		AccountCreationDTO nonexistent = new AccountCreationDTO("Nonexistent User", new BigDecimal("0.00"), "USD", 99L);
		assertThrows(UserNotFoundException.class, () -> {
			accountMapper.toAccount(nonexistent);
		});

	}

	@Test
	void testUserMapperMethods() {

		UserResponseDTO userDTO3 = userMapper.toDTO(user);
		assertEquals(user.getFirstName(), userDTO3.firstName());
		assertEquals(user.getLastName(), userDTO3.lastName());
		assertEquals(user.getAccounts().size(), userDTO3.accounts().size());
		assertTrue(user.getAccounts().contains(account) && userDTO3.accounts().contains(account.getInfo()));
		assertEquals(user.getId(), userDTO3.id());

		UserCreationDTO creationDTO = new UserCreationDTO("New", "User","owner3", "t", Role.USER);
		LedgerUser user3 = userMapper.toUser(creationDTO);

		assertEquals(creationDTO.firstName(), user3.getFirstName());
		assertEquals(creationDTO.lastName(), user3.getLastName());
	}

	@Test
	void testTransactionMapperMethods() {

		Transaction transaction = new Transaction(account2, account, new BigDecimal("800.00"), "USD");
		TransactionResponseDTO transactionDTO = transactionMapper.toDTO(transaction);

		assertEquals(transaction.getId(), transactionDTO.id());
		assertEquals(transaction.getSenderId(), transactionDTO.senderId());
		assertEquals(transaction.getReceiverId(), transactionDTO.receiverId());
		assertEquals(transaction.getAmount(), transactionDTO.amount());
		assertEquals(transaction.getCurrency(), transactionDTO.currency());
		assertEquals(transaction.getTimestamp(), transactionDTO.timestamp());
	}

	@Test
	void testDuplicateIdempotencyKeyException() {
	
		String idempotencyKey = UUID.randomUUID().toString();

		UserCreationDTO userCreationDTO = new UserCreationDTO("Idempotent", "User", "owner2", "i", Role.USER);
		userService.createUser(idempotencyKey, userCreationDTO);

		assertThrows(IdempotencyKeyAlreadyExistsException.class, () -> {
			userService.createUser(idempotencyKey, userCreationDTO);
		});

		AccountCreationDTO accountCreationDTO = new AccountCreationDTO("Idempotent Account", new BigDecimal("100.00"), "USD", user_id);
		assertThrows(IdempotencyKeyAlreadyExistsException.class, () -> {
			accountService.createAccount(idempotencyKey, user_id, accountCreationDTO);
		});
		
		UserPatchDTO userPatchDTO = new UserPatchDTO(JsonNullable.of("Idempotent"), JsonNullable.undefined());
		assertThrows(IdempotencyKeyAlreadyExistsException.class, () -> {
			userService.updateUser(idempotencyKey, user_id, userPatchDTO);
		});

		AccountPatchDTO accountPatchDTO = new AccountPatchDTO(JsonNullable.of("Idempotent Account"));
		assertThrows(IdempotencyKeyAlreadyExistsException.class, () -> {
			accountService.changeName(idempotencyKey, account_id, user_id, accountPatchDTO);
		});

		TransactionCreationDTO transactionCreationDTO = new TransactionCreationDTO(account2_id, new BigDecimal("50.00"), "USD");
		assertThrows(IdempotencyKeyAlreadyExistsException.class, () -> {
			transactionService.moneyTransfer(idempotencyKey, user_id, account_id, transactionCreationDTO);
		});
	}

}
