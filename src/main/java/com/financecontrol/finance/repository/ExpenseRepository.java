package com.financecontrol.finance.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.financecontrol.finance.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

    List<Expense> findAllByOwnerUserIdOrderByTransactionDateDescCreatedAtDesc(UUID ownerUserId);

    Optional<Expense> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    long deleteByOwnerUserId(UUID ownerUserId);

    boolean existsByRecurringTransactionIdAndOccurrenceDate(UUID recurringTransactionId, LocalDate occurrenceDate);

    boolean existsByOwnerUserIdAndCategory(UUID ownerUserId, String category);

    @Query("""
            SELECT COALESCE(SUM(expense.amount), 0)
            FROM Expense expense
            WHERE expense.ownerUserId = :ownerUserId
              AND expense.transactionDate >= :startDate
              AND expense.transactionDate < :endDate
            """)
    BigDecimal sumAmountBetween(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT expense.category AS category, SUM(expense.amount) AS total
            FROM Expense expense
            WHERE expense.ownerUserId = :ownerUserId
              AND expense.transactionDate >= :startDate
              AND expense.transactionDate < :endDate
            GROUP BY expense.category
            """)
    List<CategoryTotal> sumAmountByCategoryBetween(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    interface CategoryTotal {
        String getCategory();

        BigDecimal getTotal();
    }
}
