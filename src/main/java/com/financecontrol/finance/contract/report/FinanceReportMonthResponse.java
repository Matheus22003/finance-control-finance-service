package com.financecontrol.finance.contract.report;

import java.math.BigDecimal;
import java.time.YearMonth;

public record FinanceReportMonthResponse(
        YearMonth referenceMonth,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal balance) {
}
