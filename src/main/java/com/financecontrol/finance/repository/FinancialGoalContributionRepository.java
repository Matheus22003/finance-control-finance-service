package com.financecontrol.finance.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.financecontrol.finance.domain.FinancialGoalContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialGoalContributionRepository
        extends JpaRepository<FinancialGoalContribution, UUID> {

    List<FinancialGoalContribution>
            findAllByFinancialGoalIdAndOwnerUserIdOrderByContributionDateDescCreatedAtDesc(
                    UUID financialGoalId,
                    UUID ownerUserId);

    Optional<FinancialGoalContribution> findByIdAndFinancialGoalIdAndOwnerUserId(
            UUID id,
            UUID financialGoalId,
            UUID ownerUserId);

    List<FinancialGoalContribution>
            findAllBySourceIncomeIdAndOwnerUserIdOrderByContributionDateDescCreatedAtDesc(
                    UUID sourceIncomeId,
                    UUID ownerUserId);

    @Query("""
            SELECT COALESCE(SUM(contribution.amount), 0)
            FROM FinancialGoalContribution contribution
            WHERE contribution.ownerUserId = :ownerUserId
              AND contribution.sourceIncomeId = :sourceIncomeId
            """)
    BigDecimal sumAmountBySourceIncomeIdAndOwnerUserId(
            @Param("sourceIncomeId") UUID sourceIncomeId,
            @Param("ownerUserId") UUID ownerUserId);

    @Query("""
            SELECT contribution.sourceIncomeId AS incomeId,
                   SUM(contribution.amount) AS allocatedAmount
            FROM FinancialGoalContribution contribution
            WHERE contribution.ownerUserId = :ownerUserId
              AND contribution.sourceIncomeId IS NOT NULL
            GROUP BY contribution.sourceIncomeId
            """)
    List<IncomeAllocationTotal> sumAllocatedAmountByIncome(
            @Param("ownerUserId") UUID ownerUserId);

    interface IncomeAllocationTotal {
        UUID getIncomeId();

        BigDecimal getAllocatedAmount();
    }
}
