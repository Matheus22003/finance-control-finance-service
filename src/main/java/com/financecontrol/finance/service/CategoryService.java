package com.financecontrol.finance.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.financecontrol.finance.contract.category.CategoryRequest;
import com.financecontrol.finance.contract.category.CategoryResponse;
import com.financecontrol.finance.domain.Category;
import com.financecontrol.finance.domain.FinanceCategory;
import com.financecontrol.finance.repository.ExpenseRepository;
import com.financecontrol.finance.repository.FinanceCategoryRepository;
import com.financecontrol.finance.repository.MonthlyBudgetRepository;
import com.financecontrol.finance.repository.RecurringTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;

@Service
public class CategoryService {

    private final FinanceCategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final MonthlyBudgetRepository budgetRepository;
    private final RecurringTransactionRepository recurringRepository;
    private final JdbcTemplate jdbcTemplate;
    private volatile Boolean h2Database;

    public CategoryService(
            FinanceCategoryRepository categoryRepository,
            ExpenseRepository expenseRepository,
            MonthlyBudgetRepository budgetRepository,
            RecurringTransactionRepository recurringRepository,
            JdbcTemplate jdbcTemplate) {
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        this.recurringRepository = recurringRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public List<CategoryResponse> findAll(UUID ownerUserId) {
        return findEntities(ownerUserId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse create(UUID ownerUserId, CategoryRequest request) {
        var name = cleanName(request.name());
        var normalizedName = normalizeName(name);
        ensureDefaults(ownerUserId);
        if (categoryRepository.existsByOwnerUserIdAndNormalizedName(ownerUserId, normalizedName)) {
            throw new DomainValidationException("A category with this name already exists.");
        }

        var code = "CUSTOM_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        var category = new FinanceCategory(ownerUserId, code, name, normalizedName, false);
        return CategoryResponse.from(categoryRepository.saveAndFlush(category));
    }

    @Transactional
    public CategoryResponse update(UUID ownerUserId, Long id, CategoryRequest request) {
        var category = findEntity(ownerUserId, id);
        if (category.isDefaultCategory()) {
            throw new DomainValidationException("Default categories cannot be renamed.");
        }
        var name = cleanName(request.name());
        var normalizedName = normalizeName(name);
        if (categoryRepository.existsByOwnerUserIdAndNormalizedNameAndIdNot(
                ownerUserId,
                normalizedName,
                id)) {
            throw new DomainValidationException("A category with this name already exists.");
        }
        category.updateName(name, normalizedName);
        return CategoryResponse.from(categoryRepository.saveAndFlush(category));
    }

    @Transactional
    public void delete(UUID ownerUserId, Long id) {
        var category = findEntity(ownerUserId, id);
        if (category.isDefaultCategory()) {
            throw new DomainValidationException("Default categories cannot be deleted.");
        }
        var code = category.getCode();
        if (expenseRepository.existsByOwnerUserIdAndCategory(ownerUserId, code)
                || budgetRepository.existsByOwnerUserIdAndCategory(ownerUserId, code)
                || recurringRepository.existsByOwnerUserIdAndCategory(ownerUserId, code)) {
            throw new DomainValidationException(
                    "Categories in use cannot be deleted. Move their expenses, budgets and recurrences first.");
        }
        categoryRepository.delete(category);
    }

    @Transactional
    public String requireCategory(UUID ownerUserId, String code) {
        if (code == null || code.isBlank()) {
            throw new DomainValidationException("Category is required.");
        }
        ensureDefaults(ownerUserId);
        var normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        return categoryRepository.findByOwnerUserIdAndCode(ownerUserId, normalizedCode)
                .orElseThrow(() -> new DomainValidationException("Category does not exist."))
                .getCode();
    }

    @Transactional
    public List<FinanceCategory> findEntities(UUID ownerUserId) {
        ensureDefaults(ownerUserId);
        return categoryRepository.findAllByOwnerUserIdOrderByDefaultCategoryDescNameAsc(ownerUserId);
    }

    private FinanceCategory findEntity(UUID ownerUserId, Long id) {
        return categoryRepository.findByIdAndOwnerUserId(id, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private void ensureDefaults(UUID ownerUserId) {
        Category.defaults().stream()
                .filter(category -> !categoryRepository.existsByOwnerUserIdAndCode(
                        ownerUserId,
                        category.code()))
                .forEach(category -> insertDefaultIfMissing(
                        ownerUserId,
                        category.code(),
                        category.name(),
                        normalizeName(category.name())));
    }

    private void insertDefaultIfMissing(
            UUID ownerUserId,
            String code,
            String name,
            String normalizedName) {
        var columns = """
                owner_user_id,
                code,
                name,
                normalized_name,
                is_default,
                created_at,
                updated_at
                """;
        var values = "VALUES (?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        var sql = isH2Database()
                ? "MERGE INTO finance_categories (" + columns
                        + ") KEY (owner_user_id, code) " + values
                : "INSERT INTO finance_categories (" + columns + ") " + values
                        + " ON CONFLICT (owner_user_id, code) DO NOTHING";
        jdbcTemplate.update(sql, ownerUserId, code, name, normalizedName);
    }

    private boolean isH2Database() {
        var cached = h2Database;
        if (cached != null) {
            return cached;
        }
        var detected = Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection ->
                connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2")));
        h2Database = detected;
        return detected;
    }

    private static String cleanName(String value) {
        var name = value.trim().replaceAll("\\s+", " ");
        if (name.length() < 2) {
            throw new DomainValidationException("Category name must contain at least 2 characters.");
        }
        return name;
    }

    private static String normalizeName(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }
}
