package com.github.wcordor.ledger.dtos.userDTO;

import com.github.wcordor.ledger.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreationDTO(

    @NotBlank(message = "First name must not be blank.")
    String firstName,
    
    @NotBlank(message = "Last name must not be blank.")
    String lastName,

    @NotBlank(message = "Username must not be blank.")
    String username,

    @NotBlank(message = "Password must not be blank.")
    String password,

    @NotNull(message = "Role must not be blank.")
    Role role

) {}
