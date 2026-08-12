package com.financecontrol.finance.contract.income;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record IncomeGoalAllocationResponse(
        UUID incomeId,
        String incomeDescription,
        BigDecimal incomeAmount,
        LocalDate transactionDate,
        BigDecimal goalAllocatedAmount,
        BigDecimal goalAvailableAmount,
        List<IncomeGoalAllocationItemResponse> allocations) {

    public IncomeGoalAllocationResponse {
        allocations = List.copyOf(allocations);
    }
}
