package com.github.wcordor.ledger.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.github.wcordor.ledger.dtos.userDTO.UserResponseDTO;
import com.github.wcordor.ledger.dtos.userDTO.UserCreationDTO;
import com.github.wcordor.ledger.entity.Account;
import com.github.wcordor.ledger.entity.LedgerUser;

@Component
public class UserMapper {
    
    public UserResponseDTO toDTO(LedgerUser user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String username = user.getUsername();
        
        @SuppressWarnings("null")
        List<String> accounts = user.getAccounts()
            .stream().map(Account::getInfo).toList();

        Long id = user.getId();
        
        return new UserResponseDTO(firstName, lastName, username, accounts, id);
    }

    public LedgerUser toUser(UserCreationDTO userDTO) {
        LedgerUser user = new LedgerUser(userDTO.firstName(), userDTO.lastName(), userDTO.username(), userDTO.password(), userDTO.role());
        
        user.setFirstName(userDTO.firstName());
        user.setLastName(userDTO.lastName());
        return user;
    }
}
