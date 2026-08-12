package com.financecontrol.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.UUID;

import com.financecontrol.finance.contract.budget.BudgetCategoryResponse;
import com.financecontrol.finance.contract.budget.BudgetRequest;
import com.financecontrol.finance.contract.budget.MonthlyBudgetResponse;
import com.financecontrol.finance.domain.MonthlyBudget;
import com.financecontrol.finance.repository.ExpenseRepository;
import com.financecontrol.finance.repository.MonthlyBudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonthlyBudgetService {

    private final MonthlyBudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final RecurringTransactionService recurringTransactionService;
    private final CategoryService categoryService;
    private final Clock clock;

    public MonthlyBudgetService(
            MonthlyBudgetRepository budgetRepository,
            ExpenseRepository expenseRepository,
            RecurringTransactionService recurringTransactionService,
            CategoryService categoryService,
            Clock clock) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.recurringTransactionService = recurringTransactionService;
        this.categoryService = categoryService;
        this.clock = clock;
    }

    @Transactional
    public MonthlyBudgetResponse get(UUID ownerUserId, YearMonth month) {
        recurringTransactionService.materializeDueForUser(ownerUserId);
        var availableCategories = categoryService.findEntities(ownerUserId);
        var planned = new LinkedHashMap<String, BigDecimal>();
        var spent = new LinkedHashMap<String, BigDecimal>();
        for (var category : availableCategories) {
            planned.put(category.getCode(), currency(BigDecimal.ZERO));
            spent.put(category.getCode(), currency(BigDecimal.ZERO));
        }
        budgetRepository.findAllByOwnerUserIdAndReferenceMonthOrderByCategory(ownerUserId, month.atDay(1))
                .forEach(budget -> planned.put(budget.getCategory(), currency(budget.getAmount())));
        expenseRepository.sumAmountByCategoryBetween(ownerUserId, month.atDay(1), month.plusMonths(1).atDay(1))
                .forEach(total -> spent.put(total.getCategory(), currency(total.getTotal())));

        var categories = availableCategories.stream()
                .map(category -> categoryResponse(
                        category.getCode(),
                        category.getName(),
                        planned.get(category.getCode()),
                        spent.get(category.getCode())))
                .toList();
        var totalPlanned = categories.stream()
                .map(BudgetCategoryResponse::planned)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalSpent = categories.stream()
                .map(BudgetCategoryResponse::spent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MonthlyBudgetResponse(
                month,
                currency(totalPlanned),
                currency(totalSpent),
                currency(totalPlanned.subtract(totalSpent)),
                categories);
    }

    @Transactional
    public MonthlyBudgetResponse set(
            UUID ownerUserId,
            YearMonth month,
            String category,
            BudgetRequest request) {
        var categoryCode = categoryService.requireCategory(ownerUserId, category);
        var amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        var budget = budgetRepository
                .findByOwnerUserIdAndReferenceMonthAndCategory(ownerUserId, month.atDay(1), categoryCode)
                .orElseGet(() -> new MonthlyBudget(ownerUserId, month, categoryCode, amount));
        budget.updateAmount(amount);
        budgetRepository.saveAndFlush(budget);
        return get(ownerUserId, month);
    }

    @Transactional
    public MonthlyBudgetResponse delete(UUID ownerUserId, YearMonth month, String category) {
        var categoryCode = categoryService.requireCategory(ownerUserId, category);
        budgetRepository
                .findByOwnerUserIdAndReferenceMonthAndCategory(ownerUserId, month.atDay(1), categoryCode)
                .ifPresent(budgetRepository::delete);
        budgetRepository.flush();
        return get(ownerUserId, month);
    }

    public YearMonth currentMonth() {
        return YearMonth.now(clock);
    }

    private static BudgetCategoryResponse categoryResponse(
            String category,
            String name,
            BigDecimal planned,
            BigDecimal spent) {
        var percentage = planned.signum() == 0
                ? BigDecimal.ZERO.setScale(2)
                : spent.multiply(BigDecimal.valueOf(100))
                        .divide(planned, 2, RoundingMode.HALF_UP);
        return new BudgetCategoryResponse(
                category,
                name,
                currency(planned),
                currency(spent),
                currency(planned.subtract(spent)),
                percentage);
    }

    private static BigDecimal currency(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
}
