package com.github.wcordor.ledger.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.wcordor.ledger.dtos.userDTO.*;
import com.github.wcordor.ledger.entity.Account;
import com.github.wcordor.ledger.entity.IdempotencyKey;
import com.github.wcordor.ledger.entity.LedgerUser;
import com.github.wcordor.ledger.exception.IdempotencyKeyAlreadyExistsException;
import com.github.wcordor.ledger.exception.NullPatchFieldException;
import com.github.wcordor.ledger.exception.UserDeletionFailureException;
import com.github.wcordor.ledger.exception.UserNotFoundException;
import com.github.wcordor.ledger.exception.UsernameAlreadyExistsException;
import com.github.wcordor.ledger.mapper.UserMapper;
import com.github.wcordor.ledger.repository.IdempotencyKeyRepository;
import com.github.wcordor.ledger.repository.UserRepository;

@Service
public class UserService implements UserDetailsService {
    
    private final UserRepository repository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, IdempotencyKeyRepository idempotencyKeyRepository,
        UserMapper userMapper, BCryptPasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        LedgerUser user = repository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User " + username + "not found"));
        String password = passwordEncoder.encode(user.getPassword());

        return User.builder().username(username).password(password).roles(user.getRole().name()).build();
    }

    @Transactional
    public UserResponseDTO replaceUser(String username/*, Long id*/, UserCreationDTO userDTO) {
        LedgerUser user = repository.findWithLockingByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User " + username + "not found"));

        /*if (!user.getId().equals(id)) {
            throw new UserNotFoundException(id);
        }*/

        user.setFirstName(userDTO.firstName());
        user.setLastName(userDTO.lastName());
        user.setUsername(userDTO.username());
        user.setPassword(userDTO.password());
        user.setRole(userDTO.role());

        @SuppressWarnings("null")
        List<String> accounts = user.getAccounts().stream().map(Account::getName).toList();
        
        return new UserResponseDTO(user.getFirstName(), user.getLastName(), user.getUsername(), accounts, user.getId());
    }

    @SuppressWarnings("null")
    public List<String> getAll() {
        return repository.findAll().stream().map(LedgerUser::getName).toList();
    }

    public UserResponseDTO createUser(String idempotencyKey, UserCreationDTO userDTO) {
        IdempotencyKey savedKey = idempotencyKeyRepository.findByKey(idempotencyKey).orElse(null);

        if (savedKey != null) {
            if (savedKey.getExpiryDate().isBefore(LocalDateTime.now())) {
                idempotencyKeyRepository.delete(savedKey);
            } else {
                throw new IdempotencyKeyAlreadyExistsException();
            }
        }

        String username = userDTO.username();
        LedgerUser sameName = repository.findByUsername(username).orElse(null);

        if (sameName != null) {
            throw new UsernameAlreadyExistsException(username);
        }

        LedgerUser user = repository.save(userMapper.toUser(userDTO));

        IdempotencyKey newKey = new IdempotencyKey(idempotencyKey, LocalDateTime.now().plusHours(24));
        idempotencyKeyRepository.save(newKey);
        
        return userMapper.toDTO(user);
    }

    public UserResponseDTO getUser(/*Long id*/String username) {
        //LedgerUser user = repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        LedgerUser user = repository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
        return userMapper.toDTO(user);
    }

    public void deleteUser(/*Long id*/String username) {
        LedgerUser user = repository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));

        if (user.getAccounts().size() == 0) {
            repository.deleteById(user.getId());
        }
        else {
            throw new UserDeletionFailureException();
        }
        
    }

    @Transactional
    public UserResponseDTO updateUser(String idempotencyKey, /*Long id*/String username, UserPatchDTO userDTO) {
        IdempotencyKey savedKey = idempotencyKeyRepository.findByKey(idempotencyKey).orElse(null);

        if (savedKey != null) {
            if (savedKey.getExpiryDate().isBefore(LocalDateTime.now())) {
                idempotencyKeyRepository.delete(savedKey);
            } else {
                throw new IdempotencyKeyAlreadyExistsException();
            }
        }

        LedgerUser user = repository.findWithLockingByUsername(username).orElseThrow(() -> new UserNotFoundException(username));

        if (userDTO.getFirstName().isPresent()) {
            user.setFirstName(userDTO.getFirstName().get());

            if (userDTO.getFirstName().get() == null || userDTO.getFirstName().get().isBlank()) {
                throw new NullPatchFieldException();
            };
        }

        if (userDTO.getLastName().isPresent()) {
            user.setLastName(userDTO.getLastName().get());

            if (userDTO.getLastName().get() == null || userDTO.getLastName().get().isBlank()) {
                throw new NullPatchFieldException();
            };
        }

        IdempotencyKey newKey = new IdempotencyKey(idempotencyKey, LocalDateTime.now().plusHours(24));
        idempotencyKeyRepository.save(newKey);

        return userMapper.toDTO(user);
    }
    
}
