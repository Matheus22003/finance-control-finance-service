package com.financecontrol.finance.contract.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record FinanceReportResponse(
        YearMonth fromMonth,
        YearMonth toMonth,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal balance,
        BigDecimal savingsRatePercentage,
        long incomeCount,
        long expenseCount,
        List<FinanceReportMonthResponse> months,
        List<FinanceReportCategoryResponse> expenseCategories,
        List<FinanceReportExpenseResponse> topExpenses) {

    public FinanceReportResponse {
        months = List.copyOf(months);
        expenseCategories = List.copyOf(expenseCategories);
        topExpenses = List.copyOf(topExpenses);
    }
}
