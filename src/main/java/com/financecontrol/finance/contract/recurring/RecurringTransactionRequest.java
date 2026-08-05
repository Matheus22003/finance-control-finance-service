package com.financecontrol.finance.contract.recurring;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.financecontrol.finance.domain.Category;
import com.financecontrol.finance.domain.RecurrenceFrequency;
import com.financecontrol.finance.domain.TransactionKind;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecurringTransactionRequest(
        @NotNull(message = "Kind is required.")
        TransactionKind kind,

        @NotBlank(message = "Description is required.")
        @Size(max = 200, message = "Description must contain at most 200 characters.")
        String description,

        @NotNull(message = "Amount is required.")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01.")
        @Digits(integer = 17, fraction = 2, message = "Amount must contain at most 17 integer and 2 decimal digits.")
        BigDecimal amount,

        Category category,

        @NotNull(message = "Frequency is required.")
        RecurrenceFrequency frequency,

        @NotNull(message = "Start date is required.")
        LocalDate startDate,

        LocalDate endDate) {

    @AssertTrue(message = "Expense recurrences require a category and income recurrences cannot have one.")
    public boolean isCategoryCompatibleWithKind() {
        return kind == null || (kind == TransactionKind.EXPENSE ? category != null : category == null);
    }

    @AssertTrue(message = "End date must be on or after start date.")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
