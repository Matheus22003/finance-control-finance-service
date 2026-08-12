package com.financecontrol.finance.api;

import java.util.UUID;

import com.financecontrol.finance.contract.projection.CashFlowProjectionResponse;
import com.financecontrol.finance.service.CashFlowProjectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping(path = "/api/v1/finance/projections", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Cash-flow projections", description = "Forecasts based on recorded and recurring transactions")
public class CashFlowProjectionController {

    private static final String USER_ID_HEADER = "X-Finance-Control-User-Id";

    private final CashFlowProjectionService projectionService;

    public CashFlowProjectionController(CashFlowProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @GetMapping("/cash-flow")
    @Operation(summary = "Project monthly cash flow")
    @ApiResponse(responseCode = "400", description = "Invalid number of months")
    public CashFlowProjectionResponse project(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestParam(defaultValue = "6") @Min(1) @Max(12) int months) {
        return projectionService.project(userId, months);
    }
}
