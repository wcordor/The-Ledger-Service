package com.github.wcordor.ledger.dtos.accountDTO;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AccountCreationDTO(
    
    @NotBlank(message = "Account name must not be blank.")
    String name,

    @NotNull(message = "Initial deposit must not be null.")
    @PositiveOrZero(message = "Initial deposit cannot be negative.")
    BigDecimal initialDeposit,

    @NotBlank(message = "Please add currency.")
    String currency
) {}
