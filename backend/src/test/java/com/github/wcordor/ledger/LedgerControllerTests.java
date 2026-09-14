package com.github.wcordor.ledger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.github.wcordor.ledger.controller.UserController;
import com.github.wcordor.ledger.dtos.accountDTO.AccountCreationDTO;
import com.github.wcordor.ledger.dtos.accountDTO.AccountPatchDTO;
import com.github.wcordor.ledger.dtos.accountDTO.AccountResponseDTO;
import com.github.wcordor.ledger.dtos.transactionDTO.TransactionCreationDTO;
import com.github.wcordor.ledger.dtos.transactionDTO.TransactionResponseDTO;
import com.github.wcordor.ledger.dtos.userDTO.UserCreationDTO;
import com.github.wcordor.ledger.dtos.userDTO.UserPatchDTO;
import com.github.wcordor.ledger.dtos.userDTO.UserResponseDTO;
import com.github.wcordor.ledger.exception.AccountDeletionFailureException;
import com.github.wcordor.ledger.exception.AccountNotFoundException;
import com.github.wcordor.ledger.exception.IdempotencyKeyAlreadyExistsException;
import com.github.wcordor.ledger.exception.InsufficientFundsException;
import com.github.wcordor.ledger.exception.InvalidTransferException;
import com.github.wcordor.ledger.exception.InvalidUserIdException;
import com.github.wcordor.ledger.exception.NullPatchFieldException;
import com.github.wcordor.ledger.exception.TransactionNotFoundException;
import com.github.wcordor.ledger.exception.UserDeletionFailureException;
import com.github.wcordor.ledger.exception.UserNotFoundException;
import com.github.wcordor.ledger.service.AccountService;
import com.github.wcordor.ledger.service.TransactionService;
import com.github.wcordor.ledger.service.UserService;

import jakarta.persistence.EntityNotFoundException;

@WebMvcTest(UserController.class)
@AutoConfigureRestTestClient
class LedgerControllerTests {
    
    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void testUserGetRequest_All() {

        List<String> mockUsers = new ArrayList<String>(List.of("Mock User 1", "Mock User 2"));
        when(userService.getAll()).thenReturn(mockUsers);

        restTestClient.get().uri("/users").exchange().expectStatus().isOk().expectHeader()
            .contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$[0]")
            .isEqualTo("Mock User 1").jsonPath("$[1]").isEqualTo("Mock User 2");
    }

    @Test
    void testUserGetRequest_Single() {

        List<String> mockAccounts = new ArrayList<String>(List.of("Account 1 information", "Account 2 information"));
        UserResponseDTO mockUser = new UserResponseDTO("Mock", "GET", "mockuser", mockAccounts, 1L);

        when(userService.getUser(eq(1L))).thenReturn(mockUser);

        restTestClient.get().uri("/users/1").exchange().expectStatus().isOk().expectHeader()
            .contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("firstName")
            .isEqualTo("Mock").jsonPath("lastName").isEqualTo("GET")
            .jsonPath("accounts").isEqualTo(mockAccounts).jsonPath("id").isEqualTo(1L);

        when(userService.getUser(eq(1111L))).thenThrow(new UserNotFoundException(1111L));

        restTestClient.get().uri("/users/1111").exchange().expectStatus().isNotFound().expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo("Could not find User 1111.");
    }

    @Test
    void testUserPostRequest() {

        UserResponseDTO mockDTO = new UserResponseDTO("Mock", "POST", "mockuser", null, 3L);        
        UserCreationDTO mockBody = new UserCreationDTO("Mock", "Body", "mockuser", "mockpassword", Role.USER);

        when(userService.createUser(eq("key"), any(UserCreationDTO.class))).thenReturn(mockDTO);
        
        restTestClient.post().uri("/users").header("Idempotency-Key", "key")
            .body(mockBody).exchange().expectStatus().isCreated().expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().jsonPath("firstName").isEqualTo("Mock").jsonPath("lastName")
            .isEqualTo("POST").jsonPath("accounts").isEmpty();

        when(userService.createUser(eq("key"), any(UserCreationDTO.class)))
            .thenThrow(new IdempotencyKeyAlreadyExistsException());

        restTestClient.post().uri("/users").header("Idempotency-Key", "key").body(mockBody)
            .exchange().expectStatus().is4xxClientError().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo("Key already exists.");
        
        String firstNameInvalid = "First name must not be blank.";
        String lastNameInvalid = "Last name must not be blank.";

        restTestClient.post().uri("/users").header("Idempotency-Key", "key")
            .body(new UserCreationDTO(null, "1", "x", "p", Role.USER)).exchange().expectStatus().isBadRequest()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo(firstNameInvalid);

        restTestClient.post().uri("/users").header("Idempotency-Key", "key")
            .body(new UserCreationDTO(" ", "1", "mockuser", "mockpassword", Role.USER)).exchange().expectStatus().isBadRequest()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo(firstNameInvalid);

        restTestClient.post().uri("/users").header("Idempotency-Key", "key")
            .body(new UserCreationDTO("User", null, "mockuser", "mockpassword", Role.USER)).exchange().expectStatus().isBadRequest()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo(lastNameInvalid);

        restTestClient.post().uri("/users").header("Idempotency-Key", "key")
            .body(new UserCreationDTO("User", " ", "mockuser", "mockpassword", Role.USER)).exchange().expectStatus().isBadRequest()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo(lastNameInvalid);
        
        restTestClient.post().uri("/users").header("Idempotency-Key", "key")
            .body(new UserCreationDTO(null, null, "mockuser", "mockpassword", Role.USER)).exchange().expectStatus().isBadRequest()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .value(message -> assertTrue(message.contains(lastNameInvalid) && message.contains(firstNameInvalid)));

        restTestClient.post().uri("/users").header("Idempotency-Key", "key")
            .body(new UserCreationDTO(" ", " ", "mockuser", "mockpassword", Role.USER)).exchange().expectStatus().isBadRequest()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .value(message -> assertTrue(message.contains(lastNameInvalid) && message.contains(firstNameInvalid)));
    }

    @Test
    void testUserPutRequest() {

        List<String> mockAccounts = new ArrayList<String>(List.of("Account information"));
        UserResponseDTO mockDTO = new UserResponseDTO("Mock", "PUT", "mockuser",  mockAccounts, 3L);

        UserCreationDTO mockBody = new UserCreationDTO("Mock", "Body", "mockuser", "mockpassword", Role.USER);

        when(userService.replaceUser(eq(3L), any(UserCreationDTO.class))).thenReturn(mockDTO);

        restTestClient.put().uri("/users/3").body(mockBody).exchange().expectStatus().isOk().expectHeader()
            .contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("firstName").isEqualTo("Mock")
            .jsonPath("lastName").isEqualTo("PUT").jsonPath("accounts")
            .isEqualTo(mockAccounts);

        when(userService.replaceUser(eq(15L), any(UserCreationDTO.class))).thenThrow(new UserNotFoundException(15L));

        restTestClient.put().uri("/users/15").body(mockBody).exchange().expectStatus().isNotFound().expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo("Could not find User 15.");

        String firstNameInvalid = "First name must not be blank.";
        String lastNameInvalid = "Last name must not be blank.";

        restTestClient.put().uri("/users/1").body(new UserCreationDTO(null, "1", "mockuser", "mockpassword", Role.USER)).exchange()
            .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo(firstNameInvalid);

        restTestClient.put().uri("/users/1").body(new UserCreationDTO(" ", "1", "mockuser", "mockpassword", Role.USER)).exchange()
            .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo(firstNameInvalid);

        restTestClient.put().uri("/users/13").body(new UserCreationDTO("User", null, "mockuser", "mockpassword", Role.USER  )).exchange()
            .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo(lastNameInvalid);

        restTestClient.put().uri("/users/13").body(new UserCreationDTO("User", " ", "mockuser", "mockpassword", Role.USER)).exchange()
            .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo(lastNameInvalid);
        
        restTestClient.put().uri("/users/32").body(new UserCreationDTO(null, null, "mockuser", "mockpassword", Role.USER)).exchange()
            .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).value(message -> assertTrue(message.contains(lastNameInvalid)
                && message.contains(firstNameInvalid))
            );

        restTestClient.put().uri("/users/32").body(new UserCreationDTO(" ", " ", "mockuser", "mockpassword", Role.USER)).exchange()
            .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).value(message -> assertTrue(message.contains(lastNameInvalid)
                && message.contains(firstNameInvalid))
            );
    }

    @Test
    void testUserPatchRequest() {

        List<String> accounts = new ArrayList<>(List.of("Account 1"));
        UserResponseDTO mockDTO = new UserResponseDTO("Mock", "PATCH", "patchuser", accounts, 5L);

        UserCreationDTO mockBody = new UserCreationDTO("Mock", "Body", "mockuser", "mockpassword", Role.USER);

        when(userService.updateUser(eq("key"), eq(5L), any(UserPatchDTO.class)))
            .thenReturn(mockDTO);
        
        restTestClient.patch().uri("/users/5").header("Idempotency-Key", "key")
            .body(mockBody).exchange().expectStatus().isOk().expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().jsonPath("firstName").isEqualTo("Mock").jsonPath("lastName")
            .isEqualTo("PATCH").jsonPath("accounts").isEqualTo(accounts);

        when(userService.updateUser(eq("key"), eq(4L), any(UserPatchDTO.class)))
            .thenThrow(new IdempotencyKeyAlreadyExistsException());

        restTestClient.patch().uri("/users/4").header("Idempotency-Key", "key")
            .body(mockBody).exchange().expectStatus().is4xxClientError().expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo("Key already exists.");

        when(userService.updateUser(eq("key"), eq(3L), any(UserPatchDTO.class)))
            .thenThrow(new UserNotFoundException(3L));

        restTestClient.patch().uri("/users/3").header("Idempotency-Key", "key")
            .body(mockBody).exchange().expectStatus().isNotFound().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo("Could not find User 3.");

        when(userService.updateUser(eq("key"), eq(2L), any(UserPatchDTO.class)))
            .thenThrow(new NullPatchFieldException());

        restTestClient.patch().uri("/users/2").header("Idempotency-Key", "key")
            .body(mockBody).exchange().expectStatus().is4xxClientError().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo("The field(s) selected for change must not be null.");
    }

    @Test
    void testUserDeleteRequest() {

        restTestClient.delete().uri("/users/21/remove").exchange().expectStatus().is2xxSuccessful();

        Mockito.doThrow(new UserDeletionFailureException(34L)).when(userService).deleteUser(eq(34L));

        restTestClient.delete().uri("/users/34/remove").exchange().expectStatus().is4xxClientError()
            .expectBody(String.class).isEqualTo("User 34 could not be deleted as it still has accounts open.");
        
        Mockito.doThrow(new UserNotFoundException(9L)).when(userService).deleteUser(eq(9L));

        restTestClient.delete().uri("/users/9/remove").exchange().expectStatus().is4xxClientError()
            .expectBody(String.class).isEqualTo("Could not find User 9.");
    }

    @Test
    void testAccountGetRequest_All() {

        List<String> mockAccounts = new ArrayList<>(List.of("Mock Account 1", "Mock Account 2"));
        when(accountService.getAccounts(4L)).thenReturn(mockAccounts);

        restTestClient.get().uri("/users/4/accounts").exchange().expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$[0]")
            .isEqualTo("Mock Account 1").jsonPath("$[1]").isEqualTo("Mock Account 2");

        when(accountService.getAccounts(42L)).thenThrow(new UserNotFoundException(42L));

        restTestClient.get().uri("/users/42/accounts").exchange().expectStatus().isNotFound()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo("Could not find User 42.");
    }

    @Test
    void testAccountGetRequest_Single() {

        List<String> mockTransactions = new ArrayList<>(List.of("Mock Transaction 1", "Mock Transaction 2"));
        AccountResponseDTO mockDTO = new AccountResponseDTO("GET", new BigDecimal("1000.00"),
            "USD", "Mock User", mockTransactions, 5L);

        when(accountService.getAccount(eq(3L), eq(4L))).thenReturn(mockDTO);

        restTestClient.get().uri("/users/4/accounts/3").exchange().expectStatus().isOk().expectHeader()
            .contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("name")
            .isEqualTo("GET").jsonPath("balance").isEqualTo(new BigDecimal("1000.0"))
            .jsonPath("currency").isEqualTo("USD").jsonPath("userName")
            .isEqualTo("Mock User").jsonPath("transactions").isEqualTo(mockTransactions)
            .jsonPath("id").isEqualTo(5L);
            
        when(accountService.getAccount(eq(89L), eq(71L)))
            .thenThrow(new AccountNotFoundException(89L, 71L));

        restTestClient.get().uri("/users/71/accounts/89").exchange().expectStatus().isNotFound()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo("Account 89 and/or User 71 may not exist, or Account 89 does not belong to User 71.");
    }

    @Test
    void testAccountPostRequest() {

        AccountResponseDTO mockDTO = new AccountResponseDTO("POST", new BigDecimal("250.00"), "EUR",
            "Mock User", null, 63L);

        when(accountService.createAccount(eq("key"), eq(33L), any(AccountCreationDTO.class)))
            .thenReturn(mockDTO);

        AccountCreationDTO mockBody 
            = new AccountCreationDTO("Mock Body", new BigDecimal("0"), "MOCK", 999L);
        
        restTestClient.post().uri("/users/33/accounts").header("Idempotency-Key", "key")
            .body(mockBody).exchange().expectStatus().isCreated().expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().jsonPath("name").isEqualTo("POST").jsonPath("balance")
            .isEqualTo(new BigDecimal("250.0")).jsonPath("currency").isEqualTo("EUR")
            .jsonPath("transactions").isEmpty().jsonPath("userName").isEqualTo("Mock User")
            .jsonPath("id").isEqualTo(63L);
        
        when(accountService.createAccount(eq("key"), eq(14L), any(AccountCreationDTO.class)))
            .thenThrow(new InvalidUserIdException());

        restTestClient.post().uri("/users/14/accounts").header("Idempotency-Key", "key")
            .body(mockBody).exchange().expectStatus().is4xxClientError().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo("User ID does not match ID of Account Owner.");

        when(accountService.createAccount(eq("key"), eq(52L), any(AccountCreationDTO.class)))
            .thenThrow(new IdempotencyKeyAlreadyExistsException());

        restTestClient.post().uri("/users/52/accounts").header("Idempotency-Key", "key")
            .body(mockBody).exchange().expectStatus().is4xxClientError().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo("Key already exists.");
    }

    @Test
    void testBadAccountPostRequests() {
        
        String blankName = "Account name must not be blank.";
        String nullInitialDeposit = "Initial deposit must not be null.";
        String blankCurrency = "Please add currency.";
        String nullUserId = "User Id must not be null.";
        String negativeDeposit = "Initial deposit cannot be negative.";

        restTestClient.post().uri("/users/64/accounts").header("Idempotency-Key", "key")
            .body(new AccountCreationDTO(null, new BigDecimal("10.00"), "USD", 64L)).exchange()
            .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo(blankName);

        restTestClient.post().uri("/users/64/accounts").header("Idempotency-Key", "key")
            .body(new AccountCreationDTO(" ", new BigDecimal("10.00"), "USD", 64L)).exchange()
            .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo(blankName);

        restTestClient.post().uri("/users/78/accounts").header("Idempotency-Key", "key")
            .body(new AccountCreationDTO("Mock Body", null, "USD", 78L)).exchange()
            .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo(nullInitialDeposit);

        restTestClient.post().uri("/users/83/accounts").header("Idempotency-Key", "key")
            .body(new AccountCreationDTO("Mock Body", new BigDecimal("10.00"), null, 78L)).exchange()
            .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo(blankCurrency);

        restTestClient.post().uri("/users/83/accounts").header("Idempotency-Key", "key")
            .body(new AccountCreationDTO("Mock Body", new BigDecimal("10.00"), " ", 78L)).exchange()
            .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo(blankCurrency);

        restTestClient.post().uri("/users/83/accounts").header("Idempotency-Key", "key")
            .body(new AccountCreationDTO("Mock Body", new BigDecimal("10.00"), "USD", null)).exchange()
            .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo(nullUserId);
        
        restTestClient.post().uri("/users/99/accounts").header("Idempotency-Key", "key")
            .body(new AccountCreationDTO(null, null, null, null)).exchange()
            .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).value(message -> assertTrue(message.contains(blankName) 
                && message.contains(nullInitialDeposit) && message.contains(blankCurrency) && message.contains(nullUserId))
            );

        restTestClient.post().uri("/users/83/accounts").header("Idempotency-Key", "key")
            .body(new AccountCreationDTO("Mock Body", new BigDecimal("-10.00"), "USD", 3L)).exchange()
            .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo(negativeDeposit);
    }

    @Test
    void testAccountPatchRequest() {

        List<String> mockTransactions = new ArrayList<>(List.of("Mock Transaction 1", "Mock Transaction 2"));
        AccountResponseDTO mockDTO = new AccountResponseDTO("PATCH", new BigDecimal("250.00"), "EUR",
            "Mock User", mockTransactions, 303L);

        when(accountService.changeName(eq("key"), eq(544L), eq(303L), any(AccountPatchDTO.class)))
            .thenReturn(mockDTO);

        AccountPatchDTO mockBody 
            = new AccountPatchDTO(JsonNullable.of("Mock Body"));
        
        restTestClient.patch().uri("/users/303/accounts/544").header("Idempotency-Key", "key")
            .body(mockBody).exchange().expectStatus().isOk().expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().jsonPath("name").isEqualTo("PATCH").jsonPath("balance")
            .isEqualTo(new BigDecimal("250.0")).jsonPath("currency").isEqualTo("EUR")
            .jsonPath("transactions").isEqualTo(mockTransactions).jsonPath("userName").isEqualTo("Mock User")
            .jsonPath("id").isEqualTo(303L);
        
        when(accountService.changeName(eq("key"), eq(89L), eq(45L), any(AccountPatchDTO.class)))
            .thenThrow(new AccountNotFoundException(89L, 45L));

        restTestClient.patch().uri("/users/45/accounts/89").header("Idempotency-Key", "key")
            .body(mockBody).exchange().expectStatus().isNotFound().expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo("Account 89 and/or User 45 may not exist, or Account 89 does not belong to User 45.");

        when(accountService.changeName(eq("key"), eq(22L), eq(14L), any(AccountPatchDTO.class)))
            .thenThrow(new IdempotencyKeyAlreadyExistsException());

        restTestClient.patch().uri("/users/14/accounts/22").header("Idempotency-Key", "key")
            .body(mockBody).exchange().expectStatus().is4xxClientError().expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo("Key already exists.");

        when(accountService.changeName(eq("key"), eq(222L), eq(143L), any(AccountPatchDTO.class)))
            .thenThrow(new NullPatchFieldException());

        restTestClient.patch().uri("/users/143/accounts/222").header("Idempotency-Key", "key")
            .body(mockBody).exchange().expectStatus().is4xxClientError().expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo("The field(s) selected for change must not be null.");
    }

    @Test
    void testAccountDeleteRequest() {

        restTestClient.delete().uri("/users/21/accounts/48/remove").exchange().expectStatus().is2xxSuccessful();

        Mockito.doThrow(new AccountDeletionFailureException(48L, 21L)).when(accountService)
            .deleteAccount(eq(48L), eq(21L));

        restTestClient.delete().uri("/users/21/accounts/48/remove").exchange().expectStatus().is4xxClientError()
            .expectBody(String.class)
            .isEqualTo("Account 48 can't be deleted because it may not exist, belong to User 21, "
                + "or have an empty balance.");
        
        Mockito.doThrow(new AccountNotFoundException(91L, 36L)).when(accountService)
            .deleteAccount(eq(91L), eq(36L));

        restTestClient.delete().uri("/users/36/accounts/91/remove").exchange().expectStatus().is4xxClientError()
            .expectBody(String.class).isEqualTo("Account 91 and/or User 36 may not exist, "
                + "or Account 91 does not belong to User 36.");
    }

     @Test
    void testTransactionGetRequest_All() {

        List<String> mockTransactions = new ArrayList<>(List.of("Mock Transaction 1", "Mock Transaction 2"));
        when(transactionService.getTransactions(eq(17L), eq(4L))).thenReturn(mockTransactions);

        restTestClient.get().uri("/users/4/accounts/17/transactions").exchange().expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$[0]")
            .isEqualTo("Mock Transaction 1").jsonPath("$[1]").isEqualTo("Mock Transaction 2");

        when(transactionService.getTransactions(57L, 12L))
            .thenThrow(new AccountNotFoundException(57L, 12L));

        restTestClient.get().uri("/users/12/accounts/57/transactions").exchange().expectStatus().isNotFound()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo("Account 57 and/or User 12 may not exist, or Account 57 does not belong to User 12.");
    }

    @Test
    void testTransactionGetRequest_Single() {

        Instant timestamp = Instant.now();
        TransactionResponseDTO mockDTO = new TransactionResponseDTO(17L, 8L, new BigDecimal("100.00"),
            "USD", timestamp, 45L);

        when(transactionService.getTransaction(eq(45L), eq(17L), eq(4L))).thenReturn(mockDTO);

        restTestClient.get().uri("/users/4/accounts/17/transactions/45").exchange().expectStatus().isOk().expectHeader()
            .contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("senderId")
            .isEqualTo(17L).jsonPath("receiverId").isEqualTo(8L).jsonPath("amount")
            .isEqualTo(new BigDecimal("100.0")).jsonPath("currency").isEqualTo("USD")
            .jsonPath("timestamp").isEqualTo(timestamp.toString()).jsonPath("id").isEqualTo(45L);
            
        when(transactionService.getTransaction(eq(96L), eq(33L), eq(13L)))
            .thenThrow(new AccountNotFoundException(33L, 13L));

        restTestClient.get().uri("/users/13/accounts/33/transactions/96").exchange().expectStatus().isNotFound()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo("Account 33 and/or User 13 may not exist, or Account 33 does not belong to User 13.");

        when(transactionService.getTransaction(eq(1044L), eq(257L), eq(86L)))
            .thenThrow(new TransactionNotFoundException(1044L, 257L));

        restTestClient.get().uri("/users/86/accounts/257/transactions/1044").exchange().expectStatus().isNotFound()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo("Transaction 1044 either does not exist or does not belong to account 257.");
    }

    @Test
    void testTransactionPostRequest() {

        Instant timestamp = Instant.now();
        TransactionResponseDTO mockDTO = new TransactionResponseDTO(25L, 26L, new BigDecimal("250.00"),
            "USD", timestamp, 88L);

        when(transactionService.moneyTransfer(eq("key"), eq(13L), eq(25L), any(TransactionCreationDTO.class)))
            .thenReturn(mockDTO);

        TransactionCreationDTO mockBody 
            = new TransactionCreationDTO(1111L, new BigDecimal("1.00"), "MOCK");
        
        restTestClient.post().uri("/users/13/accounts/25/money-transfer")
            .header("Idempotency-Key", "key").body(mockBody).exchange().expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("senderId")
            .isEqualTo(25L).jsonPath("receiverId").isEqualTo(26L).jsonPath("amount")
            .isEqualTo(new BigDecimal("250.0")).jsonPath("currency").isEqualTo("USD")
            .jsonPath("timestamp").isEqualTo(timestamp.toString()).jsonPath("id").isEqualTo(88L);
        
        when(transactionService.moneyTransfer(eq("key"), eq(14L), eq(38L), any(TransactionCreationDTO.class)))
            .thenThrow(new IdempotencyKeyAlreadyExistsException());

        restTestClient.post().uri("/users/14/accounts/38/money-transfer").header("Idempotency-Key", "key")
            .body(mockBody).exchange().expectStatus().is4xxClientError().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo("Key already exists.");

        when(transactionService.moneyTransfer(eq("key"), eq(41L), eq(83L), any(TransactionCreationDTO.class)))
            .thenThrow(new InvalidTransferException());

        restTestClient.post().uri("/users/41/accounts/83/money-transfer").header("Idempotency-Key", "key")
            .body(mockBody).exchange().expectStatus().is4xxClientError().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String.class).isEqualTo("Account receiving funds must not be the account sending the funds.");

        when(transactionService.moneyTransfer(eq("key"), eq(140L), eq(549L), any(TransactionCreationDTO.class)))
            .thenThrow(new AccountNotFoundException(549L, 140L));

        restTestClient.post().uri("/users/140/accounts/549/money-transfer")
            .header("Idempotency-Key", "key").body(mockBody).exchange().expectStatus().is4xxClientError()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo("Account 549 and/or User 140 may not exist, or Account 549 does not belong to User 140.");

        when(transactionService.moneyTransfer(eq("key"), eq(1L), eq(2L), any(TransactionCreationDTO.class)))
            .thenThrow(new EntityNotFoundException("Account " + mockBody.receiverId() + " not found."));
        
        restTestClient.post().uri("/users/1/accounts/2/money-transfer")
            .header("Idempotency-Key", "key").body(mockBody).exchange().expectStatus().is4xxClientError()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo("Account 1111 not found.");    

        when(transactionService.moneyTransfer(eq("key"), eq(36L), eq(101L), any(TransactionCreationDTO.class)))
            .thenThrow(new InsufficientFundsException());

        restTestClient.post().uri("/users/36/accounts/101/money-transfer")
            .header("Idempotency-Key", "key").body(mockBody).exchange().expectStatus().is4xxClientError()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo("Not enough funds to make transaction, canceling transaction.");
    }

    @Test
    void testBadTransactionPostRequests() {

        String nullReceiverId = "Receiver Id must not be null.";
        String nullAmount = "Transfer amount must not be null.";
        String invalidAmount = "Transfer amount must be greater than 0.";
        String blankCurrency = "Please add currency.";

        restTestClient.post().uri("/users/10/accounts/40/money-transfer")
            .header("Idempotency-Key", "key")
            .body(new TransactionCreationDTO(null, new BigDecimal("80.00"), "USD")).exchange().expectStatus()
            .isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo(nullReceiverId);

        restTestClient.post().uri("/users/10/accounts/40/money-transfer")
            .header("Idempotency-Key", "key")
            .body(new TransactionCreationDTO(3L, null, "USD")).exchange().expectStatus()
            .isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo(nullAmount);

        restTestClient.post().uri("/users/10/accounts/40/money-transfer")
            .header("Idempotency-Key", "key")
            .body(new TransactionCreationDTO(3L, new BigDecimal("-80.00"), "USD")).exchange().expectStatus()
            .isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo(invalidAmount);

        restTestClient.post().uri("/users/10/accounts/40/money-transfer")
            .header("Idempotency-Key", "key")
            .body(new TransactionCreationDTO(3L, BigDecimal.ZERO, "USD")).exchange().expectStatus()
            .isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo(invalidAmount);

        restTestClient.post().uri("/users/10/accounts/40/money-transfer")
            .header("Idempotency-Key", "key")
            .body(new TransactionCreationDTO(3L, new BigDecimal("80.00"), null)).exchange().expectStatus()
            .isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo(blankCurrency);

        restTestClient.post().uri("/users/10/accounts/40/money-transfer")
            .header("Idempotency-Key", "key")
            .body(new TransactionCreationDTO(null, null, null)).exchange().expectStatus()
            .isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .value(message -> assertTrue(message.contains(nullReceiverId) && message.contains(nullAmount) && message.contains(blankCurrency)));

        restTestClient.post().uri("/users/10/accounts/40/money-transfer")
            .header("Idempotency-Key", "key")
            .body(new TransactionCreationDTO(3L, new BigDecimal("80.00"), " ")).exchange().expectStatus()
            .isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN).expectBody(String.class)
            .isEqualTo(blankCurrency);
    }
}
