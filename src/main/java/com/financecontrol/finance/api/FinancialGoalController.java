package com.financecontrol.finance.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.financecontrol.finance.contract.goal.FinancialGoalRequest;
import com.financecontrol.finance.contract.goal.FinancialGoalResponse;
import com.financecontrol.finance.service.FinancialGoalService;
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
@RequestMapping(path = "/api/v1/finance/goals", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Financial goals", description = "Personal savings goals and progress")
public class FinancialGoalController {

    private static final String USER_ID_HEADER = "X-Finance-Control-User-Id";

    private final FinancialGoalService goalService;

    public FinancialGoalController(FinancialGoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    @Operation(summary = "List financial goals")
    public List<FinancialGoalResponse> findAll(@RequestHeader(USER_ID_HEADER) UUID userId) {
        return goalService.findAll(userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a financial goal")
    @ApiResponse(responseCode = "404", description = "Financial goal not found")
    public FinancialGoalResponse findById(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id) {
        return goalService.findById(userId, id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a financial goal")
    @ApiResponse(responseCode = "201", description = "Financial goal created")
    public ResponseEntity<FinancialGoalResponse> create(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @Valid @RequestBody FinancialGoalRequest request) {
        var goal = goalService.create(userId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/finance/goals/" + goal.id()))
                .body(goal);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a financial goal")
    @ApiResponse(responseCode = "404", description = "Financial goal not found")
    public FinancialGoalResponse update(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody FinancialGoalRequest request) {
        return goalService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a financial goal")
    @ApiResponse(responseCode = "204", description = "Financial goal deleted")
    @ApiResponse(responseCode = "404", description = "Financial goal not found")
    public ResponseEntity<Void> delete(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id) {
        goalService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
