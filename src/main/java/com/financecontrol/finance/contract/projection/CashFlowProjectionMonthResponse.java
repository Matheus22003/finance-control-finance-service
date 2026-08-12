package com.financecontrol.finance.contract.projection;

import java.math.BigDecimal;
import java.time.YearMonth;

public record CashFlowProjectionMonthResponse(
        YearMonth referenceMonth,
        BigDecimal projectedIncome,
        BigDecimal projectedExpenses,
        BigDecimal projectedNet,
        BigDecimal cumulativeBalance) {
}
