package com.financecontrol.finance.contract.budget;

import java.math.BigDecimal;

public record BudgetCategoryResponse(
        String category,
        String name,
        BigDecimal planned,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal usagePercentage) {
}
