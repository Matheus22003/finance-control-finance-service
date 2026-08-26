package com.financecontrol.finance.contract.report;

import java.math.BigDecimal;

public record FinanceReportCategoryResponse(
        String category,
        String name,
        BigDecimal amount,
        BigDecimal percentage) {
}
