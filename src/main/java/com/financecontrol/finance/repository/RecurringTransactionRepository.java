package com.financecontrol.finance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.financecontrol.finance.domain.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, UUID> {

    boolean existsByOwnerUserIdAndCategory(UUID ownerUserId, String category);

    List<RecurringTransaction> findAllByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);

    List<RecurringTransaction> findAllByOwnerUserIdAndActiveTrueOrderByCreatedAtDesc(UUID ownerUserId);

    Optional<RecurringTransaction> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    List<RecurringTransaction> findAllByActiveTrueAndNextOccurrenceDateLessThanEqual(LocalDate date);

    List<RecurringTransaction> findAllByOwnerUserIdAndActiveTrueAndNextOccurrenceDateLessThanEqual(
            UUID ownerUserId,
            LocalDate date);

    long deleteByOwnerUserId(UUID ownerUserId);
}
