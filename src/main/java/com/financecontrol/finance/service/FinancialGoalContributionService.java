package com.financecontrol.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.financecontrol.finance.contract.goal.FinancialGoalContributionRequest;
import com.financecontrol.finance.contract.goal.FinancialGoalContributionResponse;
import com.financecontrol.finance.contract.goal.FinancialGoalContributionSourceResponse;
import com.financecontrol.finance.domain.FinancialGoalContribution;
import com.financecontrol.finance.domain.GoalContributionType;
import com.financecontrol.finance.repository.FinancialGoalContributionRepository;
import com.financecontrol.finance.repository.FinancialGoalRepository;
import com.financecontrol.finance.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialGoalContributionService {

    private final FinancialGoalRepository goalRepository;
    private final FinancialGoalContributionRepository contributionRepository;
    private final IncomeRepository incomeRepository;
    private final Clock clock;

    public FinancialGoalContributionService(
            FinancialGoalRepository goalRepository,
            FinancialGoalContributionRepository contributionRepository,
            IncomeRepository incomeRepository,
            Clock clock) {
        this.goalRepository = goalRepository;
        this.contributionRepository = contributionRepository;
        this.incomeRepository = incomeRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<FinancialGoalContributionResponse> findAll(UUID ownerUserId, UUID goalId) {
        findGoal(ownerUserId, goalId);
        return contributionRepository
                .findAllByFinancialGoalIdAndOwnerUserIdOrderByContributionDateDescCreatedAtDesc(
                        goalId,
                        ownerUserId)
                .stream()
                .map(FinancialGoalContributionService::toResponse)
                .toList();
    }

    @Transactional
    public FinancialGoalContributionResponse create(
            UUID ownerUserId,
            UUID goalId,
            FinancialGoalContributionRequest request) {
        if (request.contributionDate().isAfter(LocalDate.now(clock))) {
            throw new DomainValidationException("Contribution date cannot be in the future.");
        }

        var goal = findGoal(ownerUserId, goalId);
        var amount = currency(request.amount());
        var sourceIncome = request.sourceIncomeId() == null
                ? null
                : incomeRepository.findByIdAndOwnerUserIdForUpdate(
                        request.sourceIncomeId(),
                        ownerUserId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Income",
                                request.sourceIncomeId()));
        if (sourceIncome != null) {
            var allocatedAmount = contributionRepository
                    .sumAmountBySourceIncomeIdAndOwnerUserId(sourceIncome.getId(), ownerUserId);
            var availableAmount = currency(sourceIncome.getAmount()).subtract(allocatedAmount);
            if (amount.compareTo(availableAmount) > 0) {
                throw new DomainValidationException(
                        "Contribution amount exceeds the available amount of the source income.");
            }
        }
        var contribution = new FinancialGoalContribution(
                goalId,
                ownerUserId,
                amount,
                request.contributionDate(),
                normalizeNote(request.note()),
                GoalContributionType.CONTRIBUTION,
                sourceIncome == null ? null : sourceIncome.getId(),
                sourceIncome == null ? null : sourceIncome.getDescription(),
                sourceIncome == null ? null : currency(sourceIncome.getAmount()),
                sourceIncome == null ? null : sourceIncome.getTransactionDate());
        goal.addContribution(amount);
        goalRepository.save(goal);
        return toResponse(contributionRepository.saveAndFlush(contribution));
    }

    @Transactional
    public void delete(UUID ownerUserId, UUID goalId, UUID contributionId) {
        var goal = findGoal(ownerUserId, goalId);
        var contribution = contributionRepository
                .findByIdAndFinancialGoalIdAndOwnerUserId(
                        contributionId,
                        goalId,
                        ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Financial goal contribution",
                        contributionId));
        goal.removeContribution(contribution.getAmount());
        contributionRepository.delete(contribution);
        goalRepository.saveAndFlush(goal);
    }

    private com.financecontrol.finance.domain.FinancialGoal findGoal(
            UUID ownerUserId,
            UUID goalId) {
        return goalRepository.findByIdAndOwnerUserId(goalId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial goal", goalId));
    }

    private static FinancialGoalContributionResponse toResponse(
            FinancialGoalContribution contribution) {
        return new FinancialGoalContributionResponse(
                contribution.getId(),
                contribution.getFinancialGoalId(),
                currency(contribution.getAmount()),
                contribution.getContributionDate(),
                contribution.getNote(),
                contribution.getType(),
                toSourceResponse(contribution),
                contribution.getCreatedAt());
    }

    private static FinancialGoalContributionSourceResponse toSourceResponse(
            FinancialGoalContribution contribution) {
        if (contribution.getSourceIncomeDescription() == null) {
            return null;
        }
        return new FinancialGoalContributionSourceResponse(
                contribution.getSourceIncomeId(),
                contribution.getSourceIncomeDescription(),
                currency(contribution.getSourceIncomeAmount()),
                contribution.getSourceIncomeTransactionDate());
    }

    private static String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        return note.trim();
    }

    private static BigDecimal currency(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
}
