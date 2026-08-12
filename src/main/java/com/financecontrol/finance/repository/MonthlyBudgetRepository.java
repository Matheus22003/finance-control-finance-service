package com.financecontrol.finance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.financecontrol.finance.domain.MonthlyBudget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, UUID> {

    List<MonthlyBudget> findAllByOwnerUserIdAndReferenceMonthOrderByCategory(
            UUID ownerUserId,
            LocalDate referenceMonth);

    Optional<MonthlyBudget> findByOwnerUserIdAndReferenceMonthAndCategory(
            UUID ownerUserId,
            LocalDate referenceMonth,
            String category);

    boolean existsByOwnerUserIdAndCategory(UUID ownerUserId, String category);

    long deleteByOwnerUserId(UUID ownerUserId);
}
