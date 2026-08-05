package com.financecontrol.finance.api;

import java.time.YearMonth;
import java.util.UUID;

import com.financecontrol.finance.contract.MonthlySummaryResponse;
import com.financecontrol.finance.contract.FinanceTrendResponse;
import com.financecontrol.finance.service.FinanceSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping(path = "/api/v1/finance", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Finance", description = "Personal finance summaries")
@Validated
public class FinanceSummaryController {

    private static final String USER_ID_HEADER = "X-Finance-Control-User-Id";

    private final FinanceSummaryService financeSummaryService;

    public FinanceSummaryController(FinanceSummaryService financeSummaryService) {
        this.financeSummaryService = financeSummaryService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get a monthly finance summary")
    @ApiResponse(responseCode = "200", description = "Monthly summary returned successfully")
    @ApiResponse(responseCode = "400", description = "Invalid reference month")
    public MonthlySummaryResponse getSummary(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth month) {
        return month == null
                ? financeSummaryService.getCurrentMonthlySummary(userId)
                : financeSummaryService.getMonthlySummary(userId, month);
    }

    @GetMapping("/trends")
    @Operation(summary = "Get income, expense and balance trends for consecutive months")
    @ApiResponse(responseCode = "200", description = "Monthly trend returned successfully")
    @ApiResponse(responseCode = "400", description = "Invalid reference month or number of months")
    public FinanceTrendResponse getTrend(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth month,
            @RequestParam(defaultValue = "6") @Min(2) @Max(12) int months) {
        return month == null
                ? financeSummaryService.getCurrentTrend(userId, months)
                : financeSummaryService.getTrend(userId, month, months);
    }
}
