package com.financecontrol.finance.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.financecontrol.finance.domain.Income;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncomeRepository extends JpaRepository<Income, UUID>, JpaSpecificationExecutor<Income> {

    List<Income> findAllByOwnerUserIdOrderByTransactionDateDescCreatedAtDesc(UUID ownerUserId);

    Optional<Income> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    List<Income> findAllByRecurringTransactionId(UUID recurringTransactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT income
            FROM Income income
            WHERE income.id = :id
              AND income.ownerUserId = :ownerUserId
            """)
    Optional<Income> findByIdAndOwnerUserIdForUpdate(
            @Param("id") UUID id,
            @Param("ownerUserId") UUID ownerUserId);

    long deleteByOwnerUserId(UUID ownerUserId);

    boolean existsByRecurringTransactionIdAndOccurrenceDate(UUID recurringTransactionId, LocalDate occurrenceDate);

    @Query("""
            SELECT COALESCE(SUM(income.amount), 0)
            FROM Income income
            WHERE income.ownerUserId = :ownerUserId
              AND income.transactionDate >= :startDate
              AND income.transactionDate < :endDate
            """)
    BigDecimal sumAmountBetween(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
