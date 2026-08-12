package com.financecontrol.finance.service;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.financecontrol.finance.contract.expense.ExpenseRequest;
import com.financecontrol.finance.contract.expense.ExpenseResponse;
import com.financecontrol.finance.domain.Expense;
import com.financecontrol.finance.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final RecurringTransactionService recurringTransactionService;
    private final CategoryService categoryService;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            RecurringTransactionService recurringTransactionService,
            CategoryService categoryService) {
        this.expenseRepository = expenseRepository;
        this.recurringTransactionService = recurringTransactionService;
        this.categoryService = categoryService;
    }

    @Transactional
    public List<ExpenseResponse> findAll(UUID ownerUserId) {
        return findAll(ownerUserId, null, null, null);
    }

    @Transactional
    public List<ExpenseResponse> findAll(
            UUID ownerUserId,
            LocalDate fromDate,
            LocalDate toDate,
            String category) {
        validateRange(fromDate, toDate);
        recurringTransactionService.materializeDueForUser(ownerUserId);
        Specification<Expense> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("ownerUserId"), ownerUserId);
        if (fromDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), fromDate));
        }
        if (toDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), toDate));
        }
        if (category != null && !category.isBlank()) {
            var categoryCode = categoryService.requireCategory(ownerUserId, category);
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("category"), categoryCode));
        }
        var sort = Sort.by(
                Sort.Order.desc("transactionDate"),
                Sort.Order.desc("createdAt"));
        return expenseRepository.findAll(specification, sort)
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(UUID ownerUserId, UUID id) {
        return ExpenseResponse.from(findEntity(ownerUserId, id));
    }

    @Transactional
    public ExpenseResponse create(UUID ownerUserId, ExpenseRequest request) {
        var expense = new Expense(
                ownerUserId,
                request.description().trim(),
                request.amount().setScale(2, RoundingMode.UNNECESSARY),
                request.transactionDate(),
                categoryService.requireCategory(ownerUserId, request.category()));

        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse update(UUID ownerUserId, UUID id, ExpenseRequest request) {
        var expense = findEntity(ownerUserId, id);
        expense.update(
                request.description().trim(),
                request.amount().setScale(2, RoundingMode.UNNECESSARY),
                request.transactionDate(),
                categoryService.requireCategory(ownerUserId, request.category()));

        return ExpenseResponse.from(expenseRepository.saveAndFlush(expense));
    }

    @Transactional
    public void delete(UUID ownerUserId, UUID id) {
        expenseRepository.delete(findEntity(ownerUserId, id));
    }

    private Expense findEntity(UUID ownerUserId, UUID id) {
        return expenseRepository.findByIdAndOwnerUserId(id, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

    private static void validateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new DomainValidationException("From date must be on or before to date.");
        }
    }
}
