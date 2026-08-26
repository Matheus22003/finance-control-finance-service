package com.financecontrol.finance.api;

import java.time.Clock;
import java.time.YearMonth;
import java.util.UUID;

import com.financecontrol.finance.contract.report.FinanceReportResponse;
import com.financecontrol.finance.service.FinanceReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/finance/reports", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Finance reports", description = "Historical financial analysis by period")
public class FinanceReportController {

    private static final String USER_ID_HEADER = "X-Finance-Control-User-Id";

    private final FinanceReportService financeReportService;
    private final Clock clock;

    public FinanceReportController(FinanceReportService financeReportService, Clock clock) {
        this.financeReportService = financeReportService;
        this.clock = clock;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get a historical finance report")
    @ApiResponse(responseCode = "200", description = "Report returned successfully")
    @ApiResponse(responseCode = "400", description = "Invalid report period")
    public FinanceReportResponse getOverview(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth from,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth to) {
        var defaultTo = YearMonth.now(clock);
        var resolvedTo = to == null ? defaultTo : to;
        var resolvedFrom = from == null ? resolvedTo.minusMonths(5) : from;
        return financeReportService.getOverview(userId, resolvedFrom, resolvedTo);
    }
}
