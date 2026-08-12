package com.financecontrol.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.financecontrol.finance.contract.goal.FinancialGoalRequest;
import com.financecontrol.finance.contract.goal.FinancialGoalResponse;
import com.financecontrol.finance.domain.FinancialGoal;
import com.financecontrol.finance.domain.FinancialGoalContribution;
import com.financecontrol.finance.domain.GoalStatus;
import com.financecontrol.finance.domain.GoalContributionType;
import com.financecontrol.finance.repository.FinancialGoalContributionRepository;
import com.financecontrol.finance.repository.FinancialGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialGoalService {

    private final FinancialGoalRepository goalRepository;
    private final FinancialGoalContributionRepository contributionRepository;
    private final Clock clock;

    public FinancialGoalService(
            FinancialGoalRepository goalRepository,
            FinancialGoalContributionRepository contributionRepository,
            Clock clock) {
        this.goalRepository = goalRepository;
        this.contributionRepository = contributionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<FinancialGoalResponse> findAll(UUID ownerUserId) {
        var today = LocalDate.now(clock);
        return goalRepository.findAllByOwnerUserIdOrderByTargetDateAscCreatedAtAsc(ownerUserId)
                .stream()
                .map(goal -> toResponse(goal, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public FinancialGoalResponse findById(UUID ownerUserId, UUID id) {
        return toResponse(findEntity(ownerUserId, id), LocalDate.now(clock));
    }

    @Transactional
    public FinancialGoalResponse create(UUID ownerUserId, FinancialGoalRequest request) {
        var goal = new FinancialGoal(
                ownerUserId,
                request.name().trim(),
                currency(request.targetAmount()),
                currency(request.currentAmount()),
                request.targetDate());
        goal = goalRepository.saveAndFlush(goal);
        if (goal.getCurrentAmount().signum() > 0) {
            contributionRepository.save(new FinancialGoalContribution(
                    goal.getId(),
                    ownerUserId,
                    goal.getCurrentAmount(),
                    LocalDate.now(clock),
                    "Saldo inicial",
                    GoalContributionType.INITIAL));
        }
        return toResponse(goal, LocalDate.now(clock));
    }

    @Transactional
    public FinancialGoalResponse update(UUID ownerUserId, UUID id, FinancialGoalRequest request) {
        var goal = findEntity(ownerUserId, id);
        if (currency(request.currentAmount()).compareTo(goal.getCurrentAmount()) != 0) {
            throw new DomainValidationException(
                    "Current amount can only be changed through goal contributions.");
        }
        goal.update(
                request.name().trim(),
                currency(request.targetAmount()),
                request.targetDate());
        return toResponse(goalRepository.saveAndFlush(goal), LocalDate.now(clock));
    }

    @Transactional
    public void delete(UUID ownerUserId, UUID id) {
        goalRepository.delete(findEntity(ownerUserId, id));
    }

    private FinancialGoal findEntity(UUID ownerUserId, UUID id) {
        return goalRepository.findByIdAndOwnerUserId(id, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial goal", id));
    }

    private static FinancialGoalResponse toResponse(FinancialGoal goal, LocalDate today) {
        var remaining = goal.getTargetAmount()
                .subtract(goal.getCurrentAmount())
                .max(BigDecimal.ZERO);
        var progress = goal.getCurrentAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100).setScale(2));
        var status = remaining.signum() == 0
                ? GoalStatus.COMPLETED
                : goal.getTargetDate().isBefore(today)
                        ? GoalStatus.OVERDUE
                        : GoalStatus.ACTIVE;
        var remainingMonths = Math.max(
                1L,
                ChronoUnit.MONTHS.between(
                        YearMonth.from(today),
                        YearMonth.from(goal.getTargetDate())) + 1L);
        var requiredMonthlyContribution = remaining.signum() == 0
                ? currency(BigDecimal.ZERO)
                : remaining.divide(BigDecimal.valueOf(remainingMonths), 2, RoundingMode.CEILING);

        return new FinancialGoalResponse(
                goal.getId(),
                goal.getName(),
                currency(goal.getTargetAmount()),
                currency(goal.getCurrentAmount()),
                currency(remaining),
                progress,
                goal.getTargetDate(),
                status,
                requiredMonthlyContribution,
                goal.getCreatedAt(),
                goal.getUpdatedAt());
    }

    private static BigDecimal currency(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
}
