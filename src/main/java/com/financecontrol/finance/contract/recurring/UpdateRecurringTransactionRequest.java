package com.financecontrol.finance.contract.recurring;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.financecontrol.finance.domain.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateRecurringTransactionRequest(
        @NotBlank(message = "Description is required.")
        @Size(max = 200, message = "Description must contain at most 200 characters.")
        String description,

        @NotNull(message = "Amount is required.")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01.")
        @Digits(integer = 17, fraction = 2, message = "Amount must contain at most 17 integer and 2 decimal digits.")
        BigDecimal amount,

        Category category,
        LocalDate endDate,
        boolean active) {
}
