package com.financecontrol.finance.contract;

import java.time.YearMonth;
import java.util.List;

public record FinanceTrendResponse(
        YearMonth referenceMonth,
        int months,
        List<FinanceTrendMonthResponse> items) {

    public FinanceTrendResponse {
        items = List.copyOf(items);
    }
}
