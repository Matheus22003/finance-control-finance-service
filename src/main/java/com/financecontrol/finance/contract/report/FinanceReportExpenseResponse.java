package com.financecontrol.finance.contract.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FinanceReportExpenseResponse(
        UUID id,
        String description,
        BigDecimal amount,
        LocalDate transactionDate,
        String category,
        String categoryName) {
}
