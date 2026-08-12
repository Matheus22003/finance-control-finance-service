package com.financecontrol.finance.service;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.financecontrol.finance.contract.income.IncomeRequest;
import com.financecontrol.finance.contract.income.IncomeResponse;
import com.financecontrol.finance.contract.income.IncomeGoalAllocationItemResponse;
import com.financecontrol.finance.contract.income.IncomeGoalAllocationResponse;
import com.financecontrol.finance.domain.Income;
import com.financecontrol.finance.repository.IncomeRepository;
import com.financecontrol.finance.repository.FinancialGoalContributionRepository;
import com.financecontrol.finance.repository.FinancialGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final FinancialGoalContributionRepository contributionRepository;
    private final RecurringTransactionService recurringTransactionService;
    private final FinancialGoalRepository financialGoalRepository;

    public IncomeService(
            IncomeRepository incomeRepository,
            FinancialGoalContributionRepository contributionRepository,
            RecurringTransactionService recurringTransactionService,
            FinancialGoalRepository financialGoalRepository) {
        this.incomeRepository = incomeRepository;
        this.contributionRepository = contributionRepository;
        this.recurringTransactionService = recurringTransactionService;
        this.financialGoalRepository = financialGoalRepository;
    }

    @Transactional
    public List<IncomeResponse> findAll(UUID ownerUserId) {
        return findAll(ownerUserId, null, null);
    }

    @Transactional
    public List<IncomeResponse> findAll(UUID ownerUserId, LocalDate fromDate, LocalDate toDate) {
        validateRange(fromDate, toDate);
        recurringTransactionService.materializeDueForUser(ownerUserId);
        Specification<Income> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("ownerUserId"), ownerUserId);
        if (fromDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), fromDate));
        }
        if (toDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), toDate));
        }
        var sort = Sort.by(
                Sort.Order.desc("transactionDate"),
                Sort.Order.desc("createdAt"));
        Map<UUID, BigDecimal> allocations = contributionRepository
                .sumAllocatedAmountByIncome(ownerUserId)
                .stream()
                .collect(Collectors.toMap(
                        FinancialGoalContributionRepository.IncomeAllocationTotal::getIncomeId,
                        FinancialGoalContributionRepository.IncomeAllocationTotal::getAllocatedAmount));
        return incomeRepository.findAll(specification, sort)
                .stream()
                .map(income -> IncomeResponse.from(
                        income,
                        allocations.getOrDefault(income.getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional(readOnly = true)
    public IncomeResponse findById(UUID ownerUserId, UUID id) {
        return IncomeResponse.from(
                findEntity(ownerUserId, id),
                allocatedAmount(ownerUserId, id));
    }

    @Transactional(readOnly = true)
    public IncomeGoalAllocationResponse findGoalAllocations(UUID ownerUserId, UUID id) {
        var income = findEntity(ownerUserId, id);
        var contributions = contributionRepository
                .findAllBySourceIncomeIdAndOwnerUserIdOrderByContributionDateDescCreatedAtDesc(
                        id,
                        ownerUserId);
        var goalNames = financialGoalRepository.findAllById(
                        contributions.stream()
                                .map(contribution -> contribution.getFinancialGoalId())
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(
                        goal -> goal.getId(),
                        goal -> goal.getName()));
        var allocations = contributions.stream()
                .map(contribution -> new IncomeGoalAllocationItemResponse(
                        contribution.getId(),
                        contribution.getFinancialGoalId(),
                        goalNames.getOrDefault(contribution.getFinancialGoalId(), "Meta removida"),
                        contribution.getAmount(),
                        contribution.getContributionDate(),
                        contribution.getNote(),
                        contribution.getCreatedAt()))
                .toList();
        var allocatedAmount = allocations.stream()
                .map(IncomeGoalAllocationItemResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
        var incomeAmount = income.getAmount().setScale(2);
        return new IncomeGoalAllocationResponse(
                income.getId(),
                income.getDescription(),
                incomeAmount,
                income.getTransactionDate(),
                allocatedAmount,
                incomeAmount.subtract(allocatedAmount),
                allocations);
    }

    @Transactional
    public IncomeResponse create(UUID ownerUserId, IncomeRequest request) {
        var income = new Income(
                ownerUserId,
                request.description().trim(),
                request.amount().setScale(2, RoundingMode.UNNECESSARY),
                request.transactionDate());

        return IncomeResponse.from(incomeRepository.save(income), BigDecimal.ZERO);
    }

    @Transactional
    public IncomeResponse update(UUID ownerUserId, UUID id, IncomeRequest request) {
        var income = findEntityForUpdate(ownerUserId, id);
        var allocatedAmount = allocatedAmount(ownerUserId, id);
        var updatedAmount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        if (updatedAmount.compareTo(allocatedAmount) < 0) {
            throw new DomainValidationException(
                    "Income amount cannot be lower than the amount already allocated to goals.");
        }
        income.update(
                request.description().trim(),
                updatedAmount,
                request.transactionDate());

        return IncomeResponse.from(incomeRepository.saveAndFlush(income), allocatedAmount);
    }

    @Transactional
    public void delete(UUID ownerUserId, UUID id) {
        incomeRepository.delete(findEntity(ownerUserId, id));
    }

    private Income findEntity(UUID ownerUserId, UUID id) {
        return incomeRepository.findByIdAndOwnerUserId(id, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Income", id));
    }

    private Income findEntityForUpdate(UUID ownerUserId, UUID id) {
        return incomeRepository.findByIdAndOwnerUserIdForUpdate(id, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Income", id));
    }

    private BigDecimal allocatedAmount(UUID ownerUserId, UUID incomeId) {
        return contributionRepository
                .sumAmountBySourceIncomeIdAndOwnerUserId(incomeId, ownerUserId)
                .setScale(2);
    }

    private static void validateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new DomainValidationException("From date must be on or before to date.");
        }
    }
}
