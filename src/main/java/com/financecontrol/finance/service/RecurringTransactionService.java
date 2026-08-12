package com.financecontrol.finance.service;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.financecontrol.finance.contract.recurring.RecurringTransactionRequest;
import com.financecontrol.finance.contract.recurring.RecurringTransactionResponse;
import com.financecontrol.finance.contract.recurring.UpdateRecurringTransactionRequest;
import com.financecontrol.finance.domain.Expense;
import com.financecontrol.finance.domain.Income;
import com.financecontrol.finance.domain.RecurringTransaction;
import com.financecontrol.finance.domain.TransactionKind;
import com.financecontrol.finance.repository.ExpenseRepository;
import com.financecontrol.finance.repository.IncomeRepository;
import com.financecontrol.finance.repository.RecurringTransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringTransactionService {

    private static final int MAX_OCCURRENCES_PER_MATERIALIZATION = 1200;

    private final RecurringTransactionRepository recurringRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final Clock clock;
    private final CategoryService categoryService;

    public RecurringTransactionService(
            RecurringTransactionRepository recurringRepository,
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            Clock clock,
            CategoryService categoryService) {
        this.recurringRepository = recurringRepository;
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.clock = clock;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> findAll(UUID ownerUserId) {
        return recurringRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId)
                .stream()
                .map(RecurringTransactionResponse::from)
                .toList();
    }

    @Transactional
    public RecurringTransactionResponse create(UUID ownerUserId, RecurringTransactionRequest request) {
        var category = validateCategory(ownerUserId, request.kind(), request.category());
        var recurring = new RecurringTransaction(
                ownerUserId,
                request.kind(),
                request.description().trim(),
                request.amount().setScale(2, RoundingMode.UNNECESSARY),
                category,
                request.frequency(),
                request.startDate(),
                request.endDate());
        recurringRepository.saveAndFlush(recurring);
        materialize(recurring, LocalDate.now(clock));
        return RecurringTransactionResponse.from(recurring);
    }

    @Transactional
    public RecurringTransactionResponse update(
            UUID ownerUserId,
            UUID id,
            UpdateRecurringTransactionRequest request) {
        var recurring = findEntity(ownerUserId, id);
        var category = validateCategory(ownerUserId, recurring.getKind(), request.category());
        if (request.endDate() != null && request.endDate().isBefore(recurring.getStartDate())) {
            throw new DomainValidationException("End date must be on or after start date.");
        }
        recurring.update(
                request.description().trim(),
                request.amount().setScale(2, RoundingMode.UNNECESSARY),
                category,
                request.endDate(),
                request.active());
        if (recurring.isActive()) {
            materialize(recurring, LocalDate.now(clock));
        }
        return RecurringTransactionResponse.from(recurringRepository.saveAndFlush(recurring));
    }

    @Transactional
    public void delete(UUID ownerUserId, UUID id) {
        var recurring = findEntity(ownerUserId, id);
        var incomes = incomeRepository.findAllByRecurringTransactionId(recurring.getId());
        incomes.forEach(Income::detachFromRecurrence);
        incomeRepository.saveAllAndFlush(incomes);

        var expenses = expenseRepository.findAllByRecurringTransactionId(recurring.getId());
        expenses.forEach(Expense::detachFromRecurrence);
        expenseRepository.saveAllAndFlush(expenses);

        recurringRepository.delete(recurring);
        recurringRepository.flush();
    }

    @Transactional
    public void materializeDueForUser(UUID ownerUserId) {
        var today = LocalDate.now(clock);
        recurringRepository
                .findAllByOwnerUserIdAndActiveTrueAndNextOccurrenceDateLessThanEqual(ownerUserId, today)
                .forEach(recurring -> materialize(recurring, today));
    }

    @Scheduled(cron = "${finance.recurring-materialization-cron:0 5 0 * * *}")
    @Transactional
    public void materializeAllDue() {
        var today = LocalDate.now(clock);
        recurringRepository.findAllByActiveTrueAndNextOccurrenceDateLessThanEqual(today)
                .forEach(recurring -> materialize(recurring, today));
    }

    private void materialize(RecurringTransaction recurring, LocalDate cutoff) {
        var generated = 0;
        while (recurring.isActive() && !recurring.getNextOccurrenceDate().isAfter(cutoff)) {
            if (++generated > MAX_OCCURRENCES_PER_MATERIALIZATION) {
                throw new DomainValidationException("Recurring transaction has too many pending occurrences.");
            }
            var occurrenceDate = recurring.getNextOccurrenceDate();
            if (recurring.getKind() == TransactionKind.INCOME) {
                if (!incomeRepository.existsByRecurringTransactionIdAndOccurrenceDate(
                        recurring.getId(), occurrenceDate)) {
                    incomeRepository.save(new Income(
                            recurring.getOwnerUserId(),
                            recurring.getDescription(),
                            recurring.getAmount(),
                            occurrenceDate,
                            recurring.getId(),
                            occurrenceDate));
                }
            } else if (!expenseRepository.existsByRecurringTransactionIdAndOccurrenceDate(
                    recurring.getId(), occurrenceDate)) {
                expenseRepository.save(new Expense(
                        recurring.getOwnerUserId(),
                        recurring.getDescription(),
                        recurring.getAmount(),
                        occurrenceDate,
                        recurring.getCategory(),
                        recurring.getId(),
                        occurrenceDate));
            }
            recurring.advance();
        }
    }

    private RecurringTransaction findEntity(UUID ownerUserId, UUID id) {
        return recurringRepository.findByIdAndOwnerUserId(id, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction", id));
    }

    private String validateCategory(
            UUID ownerUserId,
            TransactionKind kind,
            String category) {
        if (kind == TransactionKind.EXPENSE && category == null) {
            throw new DomainValidationException("Expense recurrences require a category.");
        }
        if (kind == TransactionKind.INCOME && category != null) {
            throw new DomainValidationException("Income recurrences cannot have a category.");
        }
        return category == null ? null : categoryService.requireCategory(ownerUserId, category);
    }
}
