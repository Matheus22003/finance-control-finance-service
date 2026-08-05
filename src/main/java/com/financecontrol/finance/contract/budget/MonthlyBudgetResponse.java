package com.financecontrol.finance.contract.budget;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record MonthlyBudgetResponse(
        YearMonth referenceMonth,
        BigDecimal totalPlanned,
        BigDecimal totalSpent,
        BigDecimal totalRemaining,
        List<BudgetCategoryResponse> categories) {
}
