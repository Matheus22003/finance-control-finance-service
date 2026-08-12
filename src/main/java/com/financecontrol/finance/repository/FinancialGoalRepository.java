package com.financecontrol.finance.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.financecontrol.finance.domain.FinancialGoal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, UUID> {

    List<FinancialGoal> findAllByOwnerUserIdOrderByTargetDateAscCreatedAtAsc(UUID ownerUserId);

    Optional<FinancialGoal> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    long deleteByOwnerUserId(UUID ownerUserId);
}
