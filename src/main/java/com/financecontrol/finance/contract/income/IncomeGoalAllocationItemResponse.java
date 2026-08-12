package com.financecontrol.finance.contract.income;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record IncomeGoalAllocationItemResponse(
        UUID contributionId,
        UUID financialGoalId,
        String financialGoalName,
        BigDecimal amount,
        LocalDate contributionDate,
        String note,
        Instant createdAt) {
}
