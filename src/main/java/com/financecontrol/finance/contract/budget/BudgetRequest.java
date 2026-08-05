package com.financecontrol.finance.contract.budget;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record BudgetRequest(
        @NotNull(message = "Amount is required.")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01.")
        @Digits(integer = 17, fraction = 2, message = "Amount must contain at most 17 integer and 2 decimal digits.")
        BigDecimal amount) {
}
