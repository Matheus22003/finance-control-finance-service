package com.financecontrol.finance.contract.income;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.financecontrol.finance.domain.Income;

public record IncomeResponse(
        UUID id,
        String description,
        BigDecimal amount,
        LocalDate transactionDate,
        UUID recurringTransactionId,
        Instant createdAt,
        Instant updatedAt,
        BigDecimal goalAllocatedAmount,
        BigDecimal goalAvailableAmount) {

    public static IncomeResponse from(Income income, BigDecimal goalAllocatedAmount) {
        var allocatedAmount = goalAllocatedAmount.setScale(2);
        var availableAmount = income.getAmount().subtract(allocatedAmount).max(BigDecimal.ZERO);
        return new IncomeResponse(
                income.getId(),
                income.getDescription(),
                income.getAmount(),
                income.getTransactionDate(),
                income.getRecurringTransactionId(),
                income.getCreatedAt(),
                income.getUpdatedAt(),
                allocatedAmount,
                availableAmount.setScale(2));
    }
}
