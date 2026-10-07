package com.github.wcordor.ledger.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.github.wcordor.ledger.dtos.accountDTO.*;
import com.github.wcordor.ledger.dtos.transactionDTO.*;
import com.github.wcordor.ledger.dtos.userDTO.*;
import com.github.wcordor.ledger.service.*;

import jakarta.validation.Valid;

@RestController
public class UserController {

	private final UserService userService;
	private final AccountService accountService;
	private final TransactionService transactionService;

    public UserController(UserService userService, AccountService accountService,
		TransactionService transactionService) {

		this.userService = userService;
		this.accountService = accountService;
		this.transactionService = transactionService;
	}

    @GetMapping("/admin")
	public List<String> getUsers() {

		return userService.getAll();
	}

    @PostMapping("/admin")
	public ResponseEntity<?> newUser(@RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody UserCreationDTO userDTO) {

		return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(idempotencyKey, userDTO));
	}

	@GetMapping("/users/{username}")
	public UserResponseDTO one(@PathVariable("username") String username/*@PathVariable("id") Long id*/) {
		
		return userService.getUser(username);
	}

	@PutMapping("/admin/{username}")
	public ResponseEntity<?> replaceUser(@PathVariable("username") String username, Long id, @Valid @RequestBody UserCreationDTO userDTO) {
		
		return ResponseEntity.ok(userService.replaceUser(username, /*id, */userDTO));
	}

	@PatchMapping("/users/{username}")
	public ResponseEntity<?> updateUser(@RequestHeader("Idempotency-Key") String idempotencyKey,
		@PathVariable("username") String username/*Long id*/, @RequestBody UserPatchDTO patchDTO) {

		return ResponseEntity.ok(userService.updateUser(idempotencyKey, username, patchDTO));
	}
	

	@DeleteMapping("/users/{username}/remove")
	public ResponseEntity<?> deleteUser(/*@PathVariable("id") Long id*/@PathVariable("username") String username) {

		userService.deleteUser(username);

		return ResponseEntity.noContent().build();
	}

	@GetMapping("/users/{username}/accounts")
	public List<String> allAccounts(/*Long userId, */@PathVariable("username") String username) {

		return accountService.getAccounts(username);
	}  

	@GetMapping("/users/{id}/accounts/{accountId}")
	public AccountResponseDTO oneAccount(@PathVariable("id") Long userId, @PathVariable("accountId") Long accountId) {

		return accountService.getAccount(accountId, userId);
	}
	
	@PostMapping("users/{username}/accounts")
	public ResponseEntity<?> newAccount(@RequestHeader("Idempotency-Key") String idempotencyKey,
		/*@PathVariable("id") Long userId*/ @PathVariable("username") String username, @Valid @RequestBody AccountCreationDTO accountDTO) {

		return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(idempotencyKey, username, accountDTO));
	}

	@PatchMapping("users/{id}/accounts/{accountId}")
	public ResponseEntity<?> changeAccountName(@RequestHeader("Idempotency-Key") String idempotencyKey,
		@PathVariable("id") Long userId, @PathVariable("accountId") Long accountId, @RequestBody AccountPatchDTO patchDTO) {

		return ResponseEntity.ok(
			accountService.changeName(idempotencyKey, accountId, userId, patchDTO)
		);
	}

	@DeleteMapping("users/{id}/accounts/{accountId}/remove")
	public ResponseEntity<?> deleteAccount(@PathVariable("id") Long userId, @PathVariable("accountId") Long accountId) {

		accountService.deleteAccount(accountId, userId);

		return ResponseEntity.noContent().build();
	}

	@PostMapping("users/{id}/accounts/{accountId}/money-transfer")
	public ResponseEntity<?> newTransaction(@RequestHeader("Idempotency-Key") String idempotencyKey,
		@PathVariable("id") Long userId, @PathVariable("accountId") Long accountId, @Valid @RequestBody TransactionCreationDTO transactionDTO) {

		return ResponseEntity.status(HttpStatus.CREATED).body(
			transactionService.moneyTransfer(idempotencyKey, userId, accountId, transactionDTO)
		);
	}

	@GetMapping("users/{id}/accounts/{accountId}/transactions")
	public List<String> allTransactions(@PathVariable("id") Long userId,
		@PathVariable("accountId") Long accountId) {

		return transactionService.getTransactions(accountId, userId);
	}

	@GetMapping("users/{id}/accounts/{accountId}/transactions/{transactionId}")
    public TransactionResponseDTO oneTransaction(@PathVariable("id") Long userId,
		@PathVariable("accountId") Long accountId, @PathVariable("transactionId") Long transactionId) {

		return transactionService.getTransaction(transactionId, accountId, userId);
	}
}
