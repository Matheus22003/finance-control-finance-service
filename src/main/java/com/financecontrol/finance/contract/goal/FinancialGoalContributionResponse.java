package com.financecontrol.finance.contract.goal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.financecontrol.finance.domain.GoalContributionType;

public record FinancialGoalContributionResponse(
        UUID id,
        UUID financialGoalId,
        BigDecimal amount,
        LocalDate contributionDate,
        String note,
        GoalContributionType type,
        FinancialGoalContributionSourceResponse source,
        Instant createdAt) {
}
