package com.financecontrol.finance.contract.goal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.financecontrol.finance.domain.GoalStatus;

public record FinancialGoalResponse(
        UUID id,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        BigDecimal remainingAmount,
        BigDecimal progressPercentage,
        LocalDate targetDate,
        GoalStatus status,
        BigDecimal requiredMonthlyContribution,
        Instant createdAt,
        Instant updatedAt) {
}
