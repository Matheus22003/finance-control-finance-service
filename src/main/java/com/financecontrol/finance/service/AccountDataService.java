package com.financecontrol.finance.service;

import java.util.UUID;

import com.financecontrol.finance.repository.ExpenseRepository;
import com.financecontrol.finance.repository.IncomeRepository;
import com.financecontrol.finance.repository.MonthlyBudgetRepository;
import com.financecontrol.finance.repository.RecurringTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDataService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final MonthlyBudgetRepository monthlyBudgetRepository;

    public AccountDataService(
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            RecurringTransactionRepository recurringTransactionRepository,
            MonthlyBudgetRepository monthlyBudgetRepository) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.monthlyBudgetRepository = monthlyBudgetRepository;
    }

    @Transactional
    public void delete(UUID ownerUserId) {
        expenseRepository.deleteByOwnerUserId(ownerUserId);
        incomeRepository.deleteByOwnerUserId(ownerUserId);
        recurringTransactionRepository.deleteByOwnerUserId(ownerUserId);
        monthlyBudgetRepository.deleteByOwnerUserId(ownerUserId);
    }
}
