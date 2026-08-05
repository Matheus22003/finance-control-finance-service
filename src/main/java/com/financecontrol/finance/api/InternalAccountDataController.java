package com.financecontrol.finance.api;

import java.util.UUID;

import com.financecontrol.finance.service.AccountDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/account-data")
@Tag(name = "Internal", description = "Internal account data lifecycle")
public class InternalAccountDataController {

    private static final String USER_ID_HEADER = "X-Finance-Control-User-Id";

    private final AccountDataService accountDataService;

    public InternalAccountDataController(AccountDataService accountDataService) {
        this.accountDataService = accountDataService;
    }

    @DeleteMapping
    @Operation(summary = "Delete all private finance data owned by a user")
    @ApiResponse(responseCode = "204", description = "Account finance data deleted")
    public ResponseEntity<Void> delete(@RequestHeader(USER_ID_HEADER) UUID userId) {
        accountDataService.delete(userId);
        return ResponseEntity.noContent().build();
    }
}
