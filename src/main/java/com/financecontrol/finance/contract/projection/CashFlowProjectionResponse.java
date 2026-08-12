package com.financecontrol.finance.contract.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CashFlowProjectionResponse(
        LocalDate referenceDate,
        int months,
        BigDecimal currentRecordedBalance,
        BigDecimal totalProjectedIncome,
        BigDecimal totalProjectedExpenses,
        BigDecimal projectedCumulativeBalance,
        List<CashFlowProjectionMonthResponse> items) {

    public CashFlowProjectionResponse {
        items = List.copyOf(items);
    }
}
