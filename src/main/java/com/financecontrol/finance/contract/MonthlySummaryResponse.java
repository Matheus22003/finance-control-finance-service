package com.financecontrol.finance.contract;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record MonthlySummaryResponse(
        YearMonth referenceMonth,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal balance,
        Map<String, BigDecimal> expensesByCategory) {

    public MonthlySummaryResponse {
        expensesByCategory = Collections.unmodifiableMap(new LinkedHashMap<>(expensesByCategory));
    }
}
