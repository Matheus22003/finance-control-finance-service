package com.financecontrol.finance.api;

import java.time.YearMonth;
import java.util.UUID;

import com.financecontrol.finance.contract.budget.BudgetRequest;
import com.financecontrol.finance.contract.budget.MonthlyBudgetResponse;
import com.financecontrol.finance.service.MonthlyBudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/finance/budgets", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Monthly budgets", description = "Monthly expense limits by category")
public class MonthlyBudgetController {

    private static final String USER_ID_HEADER = "X-Finance-Control-User-Id";

    private final MonthlyBudgetService budgetService;

    public MonthlyBudgetController(MonthlyBudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    @Operation(summary = "Get monthly budget progress")
    public MonthlyBudgetResponse get(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return budgetService.get(userId, month == null ? budgetService.currentMonth() : month);
    }

    @PutMapping(path = "/{category}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create or replace a category budget")
    public MonthlyBudgetResponse set(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable String category,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @Valid @RequestBody BudgetRequest request) {
        return budgetService.set(userId, month, category, request);
    }

    @DeleteMapping("/{category}")
    @Operation(summary = "Remove a category budget")
    public MonthlyBudgetResponse delete(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable String category,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return budgetService.delete(userId, month, category);
    }
}
