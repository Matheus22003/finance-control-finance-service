package com.financecontrol.finance.contract.budget;

import java.math.BigDecimal;

import com.financecontrol.finance.domain.Category;

public record BudgetCategoryResponse(
        Category category,
        BigDecimal planned,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal usagePercentage) {
}
