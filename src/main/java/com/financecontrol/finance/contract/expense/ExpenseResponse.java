package com.financecontrol.finance.contract.expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.financecontrol.finance.domain.Expense;

public record ExpenseResponse(
        UUID id,
        String description,
        BigDecimal amount,
        LocalDate transactionDate,
        String category,
        UUID recurringTransactionId,
        Instant createdAt,
        Instant updatedAt) {

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getTransactionDate(),
                expense.getCategory(),
                expense.getRecurringTransactionId(),
                expense.getCreatedAt(),
                expense.getUpdatedAt());
    }
}
