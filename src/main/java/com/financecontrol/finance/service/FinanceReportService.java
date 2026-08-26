package com.financecontrol.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import com.financecontrol.finance.contract.report.FinanceReportCategoryResponse;
import com.financecontrol.finance.contract.report.FinanceReportExpenseResponse;
import com.financecontrol.finance.contract.report.FinanceReportMonthResponse;
import com.financecontrol.finance.contract.report.FinanceReportResponse;
import com.financecontrol.finance.repository.ExpenseRepository;
import com.financecontrol.finance.repository.IncomeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceReportService {

    private static final int MAX_MONTHS = 24;

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final RecurringTransactionService recurringTransactionService;
    private final CategoryService categoryService;

    public FinanceReportService(
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            RecurringTransactionService recurringTransactionService,
            CategoryService categoryService) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.recurringTransactionService = recurringTransactionService;
        this.categoryService = categoryService;
    }

    @Transactional
    public FinanceReportResponse getOverview(
            UUID ownerUserId,
            YearMonth fromMonth,
            YearMonth toMonth) {
        validateRange(fromMonth, toMonth);
        recurringTransactionService.materializeDueForUser(ownerUserId);

        var startDate = fromMonth.atDay(1);
        var endDateExclusive = toMonth.plusMonths(1).atDay(1);
        var totalIncome = currency(incomeRepository.sumAmountBetween(
                ownerUserId, startDate, endDateExclusive));
        var totalExpenses = currency(expenseRepository.sumAmountBetween(
                ownerUserId, startDate, endDateExclusive));
        var balance = currency(totalIncome.subtract(totalExpenses));
        var categoryNames = categoryService.findEntities(ownerUserId)
                .stream()
                .collect(Collectors.toMap(
                        category -> category.getCode(),
                        category -> category.getName(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        var months = new ArrayList<FinanceReportMonthResponse>();
        for (var month = fromMonth; !month.isAfter(toMonth); month = month.plusMonths(1)) {
            var monthStart = month.atDay(1);
            var monthEnd = month.plusMonths(1).atDay(1);
            var income = currency(incomeRepository.sumAmountBetween(
                    ownerUserId, monthStart, monthEnd));
            var expenses = currency(expenseRepository.sumAmountBetween(
                    ownerUserId, monthStart, monthEnd));
            months.add(new FinanceReportMonthResponse(
                    month,
                    income,
                    expenses,
                    currency(income.subtract(expenses))));
        }

        var expenseCategories = expenseRepository
                .sumAmountByCategoryBetween(ownerUserId, startDate, endDateExclusive)
                .stream()
                .map(total -> {
                    var amount = currency(total.getTotal());
                    return new FinanceReportCategoryResponse(
                            total.getCategory(),
                            categoryNames.getOrDefault(total.getCategory(), total.getCategory()),
                            amount,
                            percentage(amount, totalExpenses));
                })
                .sorted((left, right) -> right.amount().compareTo(left.amount()))
                .toList();

        var topExpenses = expenseRepository
                .findTopExpensesBetween(
                        ownerUserId,
                        startDate,
                        endDateExclusive,
                        PageRequest.of(0, 5))
                .stream()
                .map(expense -> new FinanceReportExpenseResponse(
                        expense.getId(),
                        expense.getDescription(),
                        currency(expense.getAmount()),
                        expense.getTransactionDate(),
                        expense.getCategory(),
                        categoryNames.getOrDefault(expense.getCategory(), expense.getCategory())))
                .toList();

        return new FinanceReportResponse(
                fromMonth,
                toMonth,
                totalIncome,
                totalExpenses,
                balance,
                totalIncome.signum() == 0 ? currency(BigDecimal.ZERO) : percentage(balance, totalIncome),
                incomeRepository.countByOwnerUserIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(
                        ownerUserId, startDate, endDateExclusive),
                expenseRepository.countByOwnerUserIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(
                        ownerUserId, startDate, endDateExclusive),
                months,
                expenseCategories,
                topExpenses);
    }

    private static void validateRange(YearMonth fromMonth, YearMonth toMonth) {
        if (fromMonth.isAfter(toMonth)) {
            throw new DomainValidationException("From month must be on or before to month.");
        }

        var monthCount = ChronoUnit.MONTHS.between(fromMonth, toMonth) + 1;
        if (monthCount > MAX_MONTHS) {
            throw new DomainValidationException("Reports support a maximum period of 24 months.");
        }
    }

    private static BigDecimal currency(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentage(BigDecimal amount, BigDecimal total) {
        if (total.signum() == 0) {
            return currency(BigDecimal.ZERO);
        }

        return amount
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }
}
