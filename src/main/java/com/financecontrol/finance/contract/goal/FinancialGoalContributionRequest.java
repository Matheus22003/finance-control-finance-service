package com.financecontrol.finance.contract.goal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FinancialGoalContributionRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull LocalDate contributionDate,
        @Size(max = 200) String note,
        UUID sourceIncomeId) {
}
