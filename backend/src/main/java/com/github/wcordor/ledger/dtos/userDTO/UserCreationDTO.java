package com.github.wcordor.ledger.dtos.userDTO;

import com.github.wcordor.ledger.Role;

import jakarta.validation.constraints.NotBlank;

public record UserCreationDTO(

    @NotBlank(message = "First name must not be blank.")
    String firstName,
    
    @NotBlank(message = "Last name must not be blank.")
    String lastName,

    @NotBlank(message = "Username must not be blank.")
    String username,

    @NotBlank(message = "Password must not be blank.")
    String password,

    @NotBlank(message = "Role must not be blank.")
    Role role

) {}
