package com.financecontrol.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

import com.financecontrol.finance.contract.projection.CashFlowProjectionMonthResponse;
import com.financecontrol.finance.contract.projection.CashFlowProjectionResponse;
import com.financecontrol.finance.domain.TransactionKind;
import com.financecontrol.finance.repository.ExpenseRepository;
import com.financecontrol.finance.repository.IncomeRepository;
import com.financecontrol.finance.repository.RecurringTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashFlowProjectionService {

    private static final int MAX_PROJECTED_OCCURRENCES_PER_RULE = 1200;

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final RecurringTransactionRepository recurringRepository;
    private final RecurringTransactionService recurringTransactionService;
    private final Clock clock;

    public CashFlowProjectionService(
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            RecurringTransactionRepository recurringRepository,
            RecurringTransactionService recurringTransactionService,
            Clock clock) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.recurringRepository = recurringRepository;
        this.recurringTransactionService = recurringTransactionService;
        this.clock = clock;
    }

    @Transactional
    public CashFlowProjectionResponse project(UUID ownerUserId, int months) {
        var today = LocalDate.now(clock);
        var firstMonth = YearMonth.from(today);
        var projectionMonths = new LinkedHashMap<YearMonth, ProjectionAmounts>();

        recurringTransactionService.materializeDueForUser(ownerUserId);
        for (var index = 0; index < months; index++) {
            var month = firstMonth.plusMonths(index);
            var startDate = month.atDay(1);
            var endDate = month.plusMonths(1).atDay(1);
            projectionMonths.put(month, new ProjectionAmounts(
                    currency(incomeRepository.sumAmountBetween(ownerUserId, startDate, endDate)),
                    currency(expenseRepository.sumAmountBetween(ownerUserId, startDate, endDate))));
        }

        var projectionEndDate = firstMonth.plusMonths(months).atDay(1).minusDays(1);
        for (var recurring : recurringRepository
                .findAllByOwnerUserIdAndActiveTrueOrderByCreatedAtDesc(ownerUserId)) {
            var occurrenceDate = recurring.getNextOccurrenceDate();
            var projectedOccurrences = 0;
            while (!occurrenceDate.isAfter(projectionEndDate) &&
                    (recurring.getEndDate() == null || !occurrenceDate.isAfter(recurring.getEndDate()))) {
                if (++projectedOccurrences > MAX_PROJECTED_OCCURRENCES_PER_RULE) {
                    throw new DomainValidationException(
                            "Recurring transaction has too many projected occurrences.");
                }
                if (occurrenceDate.isAfter(today)) {
                    var amounts = projectionMonths.get(YearMonth.from(occurrenceDate));
                    if (amounts != null) {
                        if (recurring.getKind() == TransactionKind.INCOME) {
                            amounts.income = amounts.income.add(recurring.getAmount());
                        } else {
                            amounts.expenses = amounts.expenses.add(recurring.getAmount());
                        }
                    }
                }
                occurrenceDate = recurring.getFrequency().next(occurrenceDate, recurring.getStartDate());
            }
        }

        var items = new ArrayList<CashFlowProjectionMonthResponse>(months);
        var cumulative = currency(BigDecimal.ZERO);
        var totalIncome = currency(BigDecimal.ZERO);
        var totalExpenses = currency(BigDecimal.ZERO);
        for (var entry : projectionMonths.entrySet()) {
            var income = currency(entry.getValue().income);
            var expenses = currency(entry.getValue().expenses);
            var net = currency(income.subtract(expenses));
            cumulative = currency(cumulative.add(net));
            totalIncome = currency(totalIncome.add(income));
            totalExpenses = currency(totalExpenses.add(expenses));
            items.add(new CashFlowProjectionMonthResponse(
                    entry.getKey(),
                    income,
                    expenses,
                    net,
                    cumulative));
        }

        var currentMonth = items.getFirst();
        return new CashFlowProjectionResponse(
                today,
                months,
                currentMonth.projectedNet(),
                totalIncome,
                totalExpenses,
                cumulative,
                items);
    }

    private static BigDecimal currency(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static final class ProjectionAmounts {
        private BigDecimal income;
        private BigDecimal expenses;

        private ProjectionAmounts(BigDecimal income, BigDecimal expenses) {
            this.income = income;
            this.expenses = expenses;
        }
    }
}
