package com.financecontrol.finance.contract;

import java.math.BigDecimal;
import java.time.YearMonth;

public record FinanceTrendMonthResponse(
        YearMonth referenceMonth,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal balance) {
}
