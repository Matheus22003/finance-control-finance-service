package com.financecontrol.finance.contract;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.financecontrol.finance.domain.Category;

public record MonthlySummaryResponse(
        YearMonth referenceMonth,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal balance,
        Map<Category, BigDecimal> expensesByCategory) {

    public MonthlySummaryResponse {
        expensesByCategory = Collections.unmodifiableMap(new EnumMap<>(expensesByCategory));
    }
}
