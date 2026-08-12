package com.financecontrol.finance.contract.goal;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FinancialGoalRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal targetAmount,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal currentAmount,
        @NotNull LocalDate targetDate) {
}
