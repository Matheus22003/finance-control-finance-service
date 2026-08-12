package com.financecontrol.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

import com.financecontrol.finance.contract.FinanceTrendMonthResponse;
import com.financecontrol.finance.contract.FinanceTrendResponse;
import com.financecontrol.finance.contract.MonthlySummaryResponse;
import com.financecontrol.finance.repository.ExpenseRepository;
import com.financecontrol.finance.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceSummaryService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final Clock clock;
    private final RecurringTransactionService recurringTransactionService;
    private final CategoryService categoryService;

    public FinanceSummaryService(
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            Clock clock,
            RecurringTransactionService recurringTransactionService,
            CategoryService categoryService) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.clock = clock;
        this.recurringTransactionService = recurringTransactionService;
        this.categoryService = categoryService;
    }

    @Transactional
    public MonthlySummaryResponse getCurrentMonthlySummary(UUID ownerUserId) {
        return getMonthlySummary(ownerUserId, YearMonth.now(clock));
    }

    @Transactional
    public MonthlySummaryResponse getMonthlySummary(UUID ownerUserId, YearMonth referenceMonth) {
        recurringTransactionService.materializeDueForUser(ownerUserId);
        return calculateMonthlySummary(ownerUserId, referenceMonth);
    }

    @Transactional
    public FinanceTrendResponse getTrend(UUID ownerUserId, YearMonth referenceMonth, int months) {
        recurringTransactionService.materializeDueForUser(ownerUserId);
        var items = new ArrayList<FinanceTrendMonthResponse>(months);
        var firstMonth = referenceMonth.minusMonths(months - 1L);

        for (var index = 0; index < months; index++) {
            var summary = calculateMonthlySummary(ownerUserId, firstMonth.plusMonths(index));
            items.add(new FinanceTrendMonthResponse(
                    summary.referenceMonth(),
                    summary.totalIncome(),
                    summary.totalExpenses(),
                    summary.balance()));
        }

        return new FinanceTrendResponse(referenceMonth, months, items);
    }

    @Transactional
    public FinanceTrendResponse getCurrentTrend(UUID ownerUserId, int months) {
        return getTrend(ownerUserId, YearMonth.now(clock), months);
    }

    private MonthlySummaryResponse calculateMonthlySummary(UUID ownerUserId, YearMonth referenceMonth) {
        var startDate = referenceMonth.atDay(1);
        var endDate = referenceMonth.plusMonths(1).atDay(1);
        var totalIncome = asCurrency(incomeRepository.sumAmountBetween(ownerUserId, startDate, endDate));
        var totalExpenses = asCurrency(expenseRepository.sumAmountBetween(ownerUserId, startDate, endDate));
        var expensesByCategory = new LinkedHashMap<String, BigDecimal>();
        categoryService.findEntities(ownerUserId)
                .forEach(category -> expensesByCategory.put(
                        category.getCode(),
                        asCurrency(BigDecimal.ZERO)));
        expenseRepository.sumAmountByCategoryBetween(ownerUserId, startDate, endDate)
                .forEach(total -> expensesByCategory.put(
                        total.getCategory(),
                        asCurrency(total.getTotal())));

        return new MonthlySummaryResponse(
                referenceMonth,
                totalIncome,
                totalExpenses,
                totalIncome.subtract(totalExpenses),
                expensesByCategory);
    }

    private static BigDecimal asCurrency(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
}
