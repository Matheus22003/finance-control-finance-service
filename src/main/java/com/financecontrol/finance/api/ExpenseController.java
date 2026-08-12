package com.financecontrol.finance.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.financecontrol.finance.contract.expense.ExpenseRequest;
import com.financecontrol.finance.contract.expense.ExpenseResponse;
import com.financecontrol.finance.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        path = "/api/v1/finance/expenses",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Expenses", description = "Expense management")
public class ExpenseController {

    private static final String USER_ID_HEADER = "X-Finance-Control-User-Id";

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    @Operation(summary = "List all expenses")
    public List<ExpenseResponse> findAll(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String category) {
        return expenseService.findAll(userId, from, to, category);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an expense by id")
    @ApiResponse(responseCode = "404", description = "Expense not found")
    public ExpenseResponse findById(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id) {
        return expenseService.findById(userId, id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create an expense")
    @ApiResponse(responseCode = "201", description = "Expense created")
    @ApiResponse(responseCode = "400", description = "Invalid expense")
    public ResponseEntity<ExpenseResponse> create(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @Valid @RequestBody ExpenseRequest request) {
        var expense = expenseService.create(userId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/finance/expenses/" + expense.id()))
                .body(expense);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update an expense")
    @ApiResponse(responseCode = "400", description = "Invalid expense")
    @ApiResponse(responseCode = "404", description = "Expense not found")
    public ExpenseResponse update(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an expense")
    @ApiResponse(responseCode = "204", description = "Expense deleted")
    @ApiResponse(responseCode = "404", description = "Expense not found")
    public ResponseEntity<Void> delete(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id) {
        expenseService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
