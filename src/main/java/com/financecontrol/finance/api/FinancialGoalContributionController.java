package com.financecontrol.finance.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.financecontrol.finance.contract.goal.FinancialGoalContributionRequest;
import com.financecontrol.finance.contract.goal.FinancialGoalContributionResponse;
import com.financecontrol.finance.service.FinancialGoalContributionService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        path = "/api/v1/finance/goals/{goalId}/contributions",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Financial goal contributions", description = "Immutable savings goal ledger")
public class FinancialGoalContributionController {

    private static final String USER_ID_HEADER = "X-Finance-Control-User-Id";

    private final FinancialGoalContributionService contributionService;

    public FinancialGoalContributionController(
            FinancialGoalContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @GetMapping
    @Operation(summary = "List a financial goal contribution history")
    @ApiResponse(responseCode = "404", description = "Financial goal not found")
    public List<FinancialGoalContributionResponse> findAll(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID goalId) {
        return contributionService.findAll(userId, goalId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a financial goal contribution")
    @ApiResponse(responseCode = "201", description = "Contribution registered")
    @ApiResponse(responseCode = "404", description = "Financial goal not found")
    public ResponseEntity<FinancialGoalContributionResponse> create(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID goalId,
            @Valid @RequestBody FinancialGoalContributionRequest request) {
        var contribution = contributionService.create(userId, goalId, request);
        return ResponseEntity
                .created(URI.create(
                        "/api/v1/finance/goals/" + goalId +
                                "/contributions/" + contribution.id()))
                .body(contribution);
    }

    @DeleteMapping("/{contributionId}")
    @Operation(summary = "Delete a financial goal contribution")
    @ApiResponse(responseCode = "204", description = "Contribution deleted")
    @ApiResponse(responseCode = "404", description = "Goal or contribution not found")
    public ResponseEntity<Void> delete(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID goalId,
            @PathVariable UUID contributionId) {
        contributionService.delete(userId, goalId, contributionId);
        return ResponseEntity.noContent().build();
    }
}
