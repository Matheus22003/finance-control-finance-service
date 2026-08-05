package com.financecontrol.finance.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.financecontrol.finance.contract.income.IncomeRequest;
import com.financecontrol.finance.contract.income.IncomeResponse;
import com.financecontrol.finance.service.IncomeService;
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
        path = "/api/v1/finance/incomes",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Incomes", description = "Income management")
public class IncomeController {

    private static final String USER_ID_HEADER = "X-Finance-Control-User-Id";

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    @GetMapping
    @Operation(summary = "List all incomes")
    public List<IncomeResponse> findAll(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return incomeService.findAll(userId, from, to);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an income by id")
    @ApiResponse(responseCode = "404", description = "Income not found")
    public IncomeResponse findById(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id) {
        return incomeService.findById(userId, id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create an income")
    @ApiResponse(responseCode = "201", description = "Income created")
    @ApiResponse(responseCode = "400", description = "Invalid income")
    public ResponseEntity<IncomeResponse> create(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @Valid @RequestBody IncomeRequest request) {
        var income = incomeService.create(userId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/finance/incomes/" + income.id()))
                .body(income);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update an income")
    @ApiResponse(responseCode = "400", description = "Invalid income")
    @ApiResponse(responseCode = "404", description = "Income not found")
    public IncomeResponse update(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody IncomeRequest request) {
        return incomeService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an income")
    @ApiResponse(responseCode = "204", description = "Income deleted")
    @ApiResponse(responseCode = "404", description = "Income not found")
    public ResponseEntity<Void> delete(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id) {
        incomeService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
