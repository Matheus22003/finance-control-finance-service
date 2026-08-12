package com.financecontrol.finance.contract.goal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FinancialGoalContributionSourceResponse(
        UUID incomeId,
        String description,
        BigDecimal incomeAmount,
        LocalDate transactionDate) {
}
