package com.financecontrol.finance.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.financecontrol.finance.contract.recurring.RecurringTransactionRequest;
import com.financecontrol.finance.contract.recurring.RecurringTransactionResponse;
import com.financecontrol.finance.contract.recurring.UpdateRecurringTransactionRequest;
import com.financecontrol.finance.service.RecurringTransactionService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        path = "/api/v1/finance/recurring-transactions",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Recurring transactions", description = "Recurring income and expense management")
public class RecurringTransactionController {

    private static final String USER_ID_HEADER = "X-Finance-Control-User-Id";

    private final RecurringTransactionService recurringTransactionService;

    public RecurringTransactionController(RecurringTransactionService recurringTransactionService) {
        this.recurringTransactionService = recurringTransactionService;
    }

    @GetMapping
    @Operation(summary = "List recurring transactions")
    public List<RecurringTransactionResponse> findAll(@RequestHeader(USER_ID_HEADER) UUID userId) {
        return recurringTransactionService.findAll(userId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a recurring transaction")
    @ApiResponse(responseCode = "201", description = "Recurring transaction created")
    public ResponseEntity<RecurringTransactionResponse> create(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @Valid @RequestBody RecurringTransactionRequest request) {
        var recurring = recurringTransactionService.create(userId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/finance/recurring-transactions/" + recurring.id()))
                .body(recurring);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update or pause a recurring transaction")
    public RecurringTransactionResponse update(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecurringTransactionRequest request) {
        return recurringTransactionService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a recurring rule without deleting generated entries")
    public ResponseEntity<Void> delete(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id) {
        recurringTransactionService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
