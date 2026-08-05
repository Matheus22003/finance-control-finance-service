package com.financecontrol.finance.service;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.financecontrol.finance.contract.income.IncomeRequest;
import com.financecontrol.finance.contract.income.IncomeResponse;
import com.financecontrol.finance.domain.Income;
import com.financecontrol.finance.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final RecurringTransactionService recurringTransactionService;

    public IncomeService(
            IncomeRepository incomeRepository,
            RecurringTransactionService recurringTransactionService) {
        this.incomeRepository = incomeRepository;
        this.recurringTransactionService = recurringTransactionService;
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
        return incomeRepository.findAll(specification, sort)
                .stream()
                .map(IncomeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncomeResponse findById(UUID ownerUserId, UUID id) {
        return IncomeResponse.from(findEntity(ownerUserId, id));
    }

    @Transactional
    public IncomeResponse create(UUID ownerUserId, IncomeRequest request) {
        var income = new Income(
                ownerUserId,
                request.description().trim(),
                request.amount().setScale(2, RoundingMode.UNNECESSARY),
                request.transactionDate());

        return IncomeResponse.from(incomeRepository.save(income));
    }

    @Transactional
    public IncomeResponse update(UUID ownerUserId, UUID id, IncomeRequest request) {
        var income = findEntity(ownerUserId, id);
        income.update(
                request.description().trim(),
                request.amount().setScale(2, RoundingMode.UNNECESSARY),
                request.transactionDate());

        return IncomeResponse.from(incomeRepository.saveAndFlush(income));
    }

    @Transactional
    public void delete(UUID ownerUserId, UUID id) {
        incomeRepository.delete(findEntity(ownerUserId, id));
    }

    private Income findEntity(UUID ownerUserId, UUID id) {
        return incomeRepository.findByIdAndOwnerUserId(id, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Income", id));
    }

    private static void validateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new DomainValidationException("From date must be on or before to date.");
        }
    }
}
