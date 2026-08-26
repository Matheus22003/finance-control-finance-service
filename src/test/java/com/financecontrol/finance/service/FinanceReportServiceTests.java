package com.financecontrol.finance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import com.financecontrol.finance.domain.Expense;
import com.financecontrol.finance.domain.FinanceCategory;
import com.financecontrol.finance.repository.ExpenseRepository;
import com.financecontrol.finance.repository.IncomeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class FinanceReportServiceTests {

    @Test
    void calculatesHistoricalOverviewWithCategoriesAndTopExpenses() {
        var incomeRepository = mock(IncomeRepository.class);
        var expenseRepository = mock(ExpenseRepository.class);
        var recurringTransactionService = mock(RecurringTransactionService.class);
        var categoryService = mock(CategoryService.class);
        var foodCategory = mock(FinanceCategory.class);
        var foodTotal = mock(ExpenseRepository.CategoryTotal.class);
        var userId = UUID.fromString("7f805b46-0b56-4a5d-86eb-d4f53c92db93");
        var periodStart = LocalDate.of(2026, 6, 1);
        var periodEnd = LocalDate.of(2026, 8, 1);

        when(incomeRepository.sumAmountBetween(userId, periodStart, periodEnd))
                .thenReturn(new BigDecimal("2200.00"));
        when(expenseRepository.sumAmountBetween(userId, periodStart, periodEnd))
                .thenReturn(new BigDecimal("1300.00"));
        when(incomeRepository.sumAmountBetween(
                userId,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1))).thenReturn(new BigDecimal("1000.00"));
        when(expenseRepository.sumAmountBetween(
                userId,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1))).thenReturn(new BigDecimal("600.00"));
        when(incomeRepository.sumAmountBetween(
                userId,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1))).thenReturn(new BigDecimal("1200.00"));
        when(expenseRepository.sumAmountBetween(
                userId,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1))).thenReturn(new BigDecimal("700.00"));
        when(incomeRepository
                .countByOwnerUserIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(
                        userId, periodStart, periodEnd)).thenReturn(2L);
        when(expenseRepository
                .countByOwnerUserIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(
                        userId, periodStart, periodEnd)).thenReturn(3L);
        when(foodCategory.getCode()).thenReturn("FOOD");
        when(foodCategory.getName()).thenReturn("Alimentação");
        when(categoryService.findEntities(userId)).thenReturn(List.of(foodCategory));
        when(foodTotal.getCategory()).thenReturn("FOOD");
        when(foodTotal.getTotal()).thenReturn(new BigDecimal("900.00"));
        when(expenseRepository.sumAmountByCategoryBetween(userId, periodStart, periodEnd))
                .thenReturn(List.of(foodTotal));
        when(expenseRepository.findTopExpensesBetween(
                eq(userId),
                eq(periodStart),
                eq(periodEnd),
                any(Pageable.class))).thenReturn(List.of(new Expense(
                        userId,
                        "Supermercado",
                        new BigDecimal("450.00"),
                        LocalDate.of(2026, 7, 15),
                        "FOOD")));

        var service = new FinanceReportService(
                incomeRepository,
                expenseRepository,
                recurringTransactionService,
                categoryService);

        var report = service.getOverview(userId, YearMonth.of(2026, 6), YearMonth.of(2026, 7));

        assertEquals(new BigDecimal("2200.00"), report.totalIncome());
        assertEquals(new BigDecimal("1300.00"), report.totalExpenses());
        assertEquals(new BigDecimal("900.00"), report.balance());
        assertEquals(new BigDecimal("40.91"), report.savingsRatePercentage());
        assertEquals(2L, report.incomeCount());
        assertEquals(3L, report.expenseCount());
        assertEquals(2, report.months().size());
        assertEquals(new BigDecimal("400.00"), report.months().getFirst().balance());
        assertEquals(new BigDecimal("500.00"), report.months().getLast().balance());
        assertEquals("Alimentação", report.expenseCategories().getFirst().name());
        assertEquals(new BigDecimal("69.23"), report.expenseCategories().getFirst().percentage());
        assertEquals("Supermercado", report.topExpenses().getFirst().description());
        verify(recurringTransactionService, times(1)).materializeDueForUser(userId);
    }

    @Test
    void rejectsPeriodsLongerThanTwentyFourMonths() {
        var service = new FinanceReportService(
                mock(IncomeRepository.class),
                mock(ExpenseRepository.class),
                mock(RecurringTransactionService.class),
                mock(CategoryService.class));

        var exception = assertThrows(
                DomainValidationException.class,
                () -> service.getOverview(
                        UUID.randomUUID(),
                        YearMonth.of(2024, 1),
                        YearMonth.of(2026, 1)));

        assertEquals("Reports support a maximum period of 24 months.", exception.getMessage());
    }
}
