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

    @PostMapping("/users")
	public ResponseEntity<?> newUser(@RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody UserCreationDTO userDTO) {

		return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(idempotencyKey, userDTO));
	}

	@GetMapping("/users/{id}")
	public UserResponseDTO one(@PathVariable("id") Long id) {
		
		return userService.getUser(id);
	}

	@PutMapping("/users/{id}")
	public ResponseEntity<?> replaceUser(@PathVariable Long id, @Valid @RequestBody UserCreationDTO userDTO) {
		
		return ResponseEntity.ok(userService.replaceUser(id, userDTO));
	}

	@PatchMapping("/users/{id}")
	public ResponseEntity<?> updateUser(@RequestHeader("Idempotency-Key") String idempotencyKey,
		@PathVariable Long id, @RequestBody UserPatchDTO patchDTO) {

		return ResponseEntity.ok(userService.updateUser(idempotencyKey, id, patchDTO));
	}
	

	@DeleteMapping("/users/{id}/remove")
	public ResponseEntity<?> deleteUser(@PathVariable("id") Long id) {

		userService.deleteUser(id);

		return ResponseEntity.noContent().build();
	}

	@GetMapping("/users/{id}/accounts")
	public List<String> allAccounts(@PathVariable("id") Long userId) {

		return accountService.getAccounts(userId);
	}  

	@GetMapping("/users/{id}/accounts/{accountId}")
	public AccountResponseDTO oneAccount(@PathVariable("id") Long userId, @PathVariable("accountId") Long accountId) {

		return accountService.getAccount(accountId, userId);
	}
	
	@PostMapping("users/{id}/accounts")
	public ResponseEntity<?> newAccount(@RequestHeader("Idempotency-Key") String idempotencyKey,
		@PathVariable("id") Long userId, @Valid @RequestBody AccountCreationDTO accountDTO) {

		return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(idempotencyKey, userId, accountDTO));
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
