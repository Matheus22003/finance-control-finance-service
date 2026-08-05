package com.financecontrol.finance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.financecontrol.finance.domain.Category;
import com.financecontrol.finance.repository.ExpenseRepository;
import com.financecontrol.finance.repository.IncomeRepository;
import org.junit.jupiter.api.Test;

class FinanceSummaryServiceTests {

    @Test
    void calculatesADeterministicMonthlySummary() {
        var incomeRepository = mock(IncomeRepository.class);
        var expenseRepository = mock(ExpenseRepository.class);
        var recurringTransactionService = mock(RecurringTransactionService.class);
        var foodTotal = mock(ExpenseRepository.CategoryTotal.class);
        var startDate = LocalDate.of(2026, 7, 1);
        var endDate = LocalDate.of(2026, 8, 1);
        var userId = UUID.fromString("7f805b46-0b56-4a5d-86eb-d4f53c92db93");

        when(incomeRepository.sumAmountBetween(userId, startDate, endDate))
                .thenReturn(new BigDecimal("5000.00"));
        when(expenseRepository.sumAmountBetween(userId, startDate, endDate))
                .thenReturn(new BigDecimal("650.00"));
        when(foodTotal.getCategory()).thenReturn(Category.FOOD);
        when(foodTotal.getTotal()).thenReturn(new BigDecimal("650.00"));
        when(expenseRepository.sumAmountByCategoryBetween(userId, startDate, endDate))
                .thenReturn(List.of(foodTotal));
        doNothing().when(recurringTransactionService).materializeDueForUser(userId);

        var clock = Clock.fixed(Instant.parse("2026-07-31T12:00:00Z"), ZoneOffset.UTC);
        var service = new FinanceSummaryService(
                incomeRepository,
                expenseRepository,
                clock,
                recurringTransactionService);

        var summary = service.getCurrentMonthlySummary(userId);

        assertEquals("2026-07", summary.referenceMonth().toString());
        assertEquals(new BigDecimal("5000.00"), summary.totalIncome());
        assertEquals(new BigDecimal("650.00"), summary.totalExpenses());
        assertEquals(new BigDecimal("4350.00"), summary.balance());
        assertEquals(new BigDecimal("650.00"), summary.expensesByCategory().get(Category.FOOD));
        assertEquals(new BigDecimal("0.00"), summary.expensesByCategory().get(Category.RENT));
    }

    @Test
    void calculatesAChronologicalTrendAndMaterializesRecurrencesOnce() {
        var incomeRepository = mock(IncomeRepository.class);
        var expenseRepository = mock(ExpenseRepository.class);
        var recurringTransactionService = mock(RecurringTransactionService.class);
        var userId = UUID.fromString("7f805b46-0b56-4a5d-86eb-d4f53c92db93");

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
        when(expenseRepository.sumAmountByCategoryBetween(
                userId,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1))).thenReturn(List.of());
        when(expenseRepository.sumAmountByCategoryBetween(
                userId,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1))).thenReturn(List.of());

        var service = new FinanceSummaryService(
                incomeRepository,
                expenseRepository,
                Clock.fixed(Instant.parse("2026-07-31T12:00:00Z"), ZoneOffset.UTC),
                recurringTransactionService);

        var trend = service.getCurrentTrend(userId, 2);

        assertEquals("2026-07", trend.referenceMonth().toString());
        assertEquals(2, trend.items().size());
        assertEquals("2026-06", trend.items().getFirst().referenceMonth().toString());
        assertEquals(new BigDecimal("400.00"), trend.items().getFirst().balance());
        assertEquals("2026-07", trend.items().getLast().referenceMonth().toString());
        assertEquals(new BigDecimal("500.00"), trend.items().getLast().balance());
        verify(recurringTransactionService, times(1)).materializeDueForUser(userId);
    }
}
