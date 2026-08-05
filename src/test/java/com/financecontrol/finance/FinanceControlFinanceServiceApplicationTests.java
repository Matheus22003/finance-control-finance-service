package com.financecontrol.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.regex.Pattern;

import com.financecontrol.finance.repository.ExpenseRepository;
import com.financecontrol.finance.repository.IncomeRepository;
import com.financecontrol.finance.repository.MonthlyBudgetRepository;
import com.financecontrol.finance.repository.RecurringTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FinanceControlFinanceServiceApplicationTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"");
    private static final String DEMO_USER_ID = "7f805b46-0b56-4a5d-86eb-d4f53c92db93";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    @LocalServerPort
    private int port;

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private MonthlyBudgetRepository monthlyBudgetRepository;

    @BeforeEach
    void cleanDatabase() {
        expenseRepository.deleteAll();
        incomeRepository.deleteAll();
        recurringTransactionRepository.deleteAll();
        monthlyBudgetRepository.deleteAll();
    }

    @Test
    void healthEndpointIsAvailable() throws IOException, InterruptedException {
        var response = get("/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/"));
        assertTrue(response.body().contains("\"status\":\"UP\""));
    }

    @Test
    void incomeCrudPersistsChanges() throws IOException, InterruptedException {
        var createResponse = post("/api/v1/finance/incomes", """
                {
                  "description": "Salary",
                  "amount": 5000.00,
                  "transactionDate": "2026-07-05"
                }
                """);

        assertEquals(201, createResponse.statusCode());
        var id = extractId(createResponse.body());
        assertTrue(createResponse.headers().firstValue("location").orElse("").endsWith("/" + id));

        var getResponse = get("/api/v1/finance/incomes/" + id);
        assertEquals(200, getResponse.statusCode());
        assertTrue(getResponse.body().contains("\"description\":\"Salary\""));
        assertTrue(getResponse.body().contains("\"amount\":5000.00"));

        var updateResponse = put("/api/v1/finance/incomes/" + id, """
                {
                  "description": "Updated salary",
                  "amount": 5250.00,
                  "transactionDate": "2026-07-05"
                }
                """);
        assertEquals(200, updateResponse.statusCode());
        assertTrue(updateResponse.body().contains("\"description\":\"Updated salary\""));
        assertTrue(updateResponse.body().contains("\"amount\":5250.00"));

        var listResponse = get("/api/v1/finance/incomes");
        assertEquals(200, listResponse.statusCode());
        assertTrue(listResponse.body().contains(id));

        assertEquals(204, delete("/api/v1/finance/incomes/" + id).statusCode());
        var notFoundResponse = get("/api/v1/finance/incomes/" + id);
        assertEquals(404, notFoundResponse.statusCode());
        assertTrue(notFoundResponse.body().contains("\"title\":\"Resource not found\""));
    }

    @Test
    void financeDataIsIsolatedByUser() throws IOException, InterruptedException {
        var createResponse = post("/api/v1/finance/incomes", """
                {
                  "description": "Private salary",
                  "amount": 1000.00,
                  "transactionDate": "2026-07-05"
                }
                """);
        assertEquals(201, createResponse.statusCode());
        var id = extractId(createResponse.body());

        var anotherUserId = "8750c27d-a3ff-4c8f-997b-c6f230005040";
        var listResponse = sendAsUser("GET", "/api/v1/finance/incomes", null, anotherUserId);
        var getResponse = sendAsUser("GET", "/api/v1/finance/incomes/" + id, null, anotherUserId);

        assertEquals(200, listResponse.statusCode());
        assertEquals("[]", listResponse.body());
        assertEquals(404, getResponse.statusCode());
    }

    @Test
    void internalAccountDeletionRemovesOnlyTheRequestedUsersData()
            throws IOException, InterruptedException {
        assertEquals(201, post("/api/v1/finance/incomes", """
                {
                  "description": "Private salary",
                  "amount": 1000.00,
                  "transactionDate": "2026-07-05"
                }
                """).statusCode());
        assertEquals(201, post("/api/v1/finance/expenses", """
                {
                  "description": "Private expense",
                  "amount": 100.00,
                  "transactionDate": "2026-07-06",
                  "category": "OTHER"
                }
                """).statusCode());
        var anotherUserId = "8750c27d-a3ff-4c8f-997b-c6f230005040";
        assertEquals(201, sendAsUser("POST", "/api/v1/finance/incomes", """
                {
                  "description": "Other salary",
                  "amount": 2000.00,
                  "transactionDate": "2026-07-05"
                }
                """, anotherUserId).statusCode());

        assertEquals(204, delete("/api/v1/internal/account-data").statusCode());
        assertEquals("[]", get("/api/v1/finance/incomes").body());
        assertEquals("[]", get("/api/v1/finance/expenses").body());
        assertTrue(sendAsUser("GET", "/api/v1/finance/incomes", null, anotherUserId)
                .body().contains("Other salary"));
        assertEquals(204, delete("/api/v1/internal/account-data").statusCode());
    }

    @Test
    void expenseCrudPersistsChanges() throws IOException, InterruptedException {
        var createResponse = post("/api/v1/finance/expenses", """
                {
                  "description": "Groceries",
                  "amount": 650.00,
                  "transactionDate": "2026-07-10",
                  "category": "FOOD"
                }
                """);

        assertEquals(201, createResponse.statusCode());
        var id = extractId(createResponse.body());

        var updateResponse = put("/api/v1/finance/expenses/" + id, """
                {
                  "description": "Monthly groceries",
                  "amount": 700.00,
                  "transactionDate": "2026-07-10",
                  "category": "OTHER"
                }
                """);
        assertEquals(200, updateResponse.statusCode());
        assertTrue(updateResponse.body().contains("\"category\":\"OTHER\""));
        assertTrue(updateResponse.body().contains("\"amount\":700.00"));

        var listResponse = get("/api/v1/finance/expenses");
        assertEquals(200, listResponse.statusCode());
        assertTrue(listResponse.body().contains(id));

        assertEquals(204, delete("/api/v1/finance/expenses/" + id).statusCode());
        assertEquals(404, get("/api/v1/finance/expenses/" + id).statusCode());
    }

    @Test
    void monthlySummaryIsCalculatedFromPersistedData() throws IOException, InterruptedException {
        post("/api/v1/finance/incomes", """
                {
                  "description": "Salary",
                  "amount": 5000.00,
                  "transactionDate": "2026-07-05"
                }
                """);
        post("/api/v1/finance/incomes", """
                {
                  "description": "Freelance",
                  "amount": 500.00,
                  "transactionDate": "2026-07-20"
                }
                """);
        post("/api/v1/finance/expenses", """
                {
                  "description": "Groceries",
                  "amount": 650.00,
                  "transactionDate": "2026-07-10",
                  "category": "FOOD"
                }
                """);
        post("/api/v1/finance/expenses", """
                {
                  "description": "Rent",
                  "amount": 1800.00,
                  "transactionDate": "2026-07-01",
                  "category": "RENT"
                }
                """);
        post("/api/v1/finance/expenses", """
                {
                  "description": "Previous month expense",
                  "amount": 99.00,
                  "transactionDate": "2026-06-30",
                  "category": "OTHER"
                }
                """);

        var response = get("/api/v1/finance/summary?month=2026-07");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"referenceMonth\":\"2026-07\""), response.body());
        assertTrue(response.body().contains("\"totalIncome\":5500.00"), response.body());
        assertTrue(response.body().contains("\"totalExpenses\":2450.00"), response.body());
        assertTrue(response.body().contains("\"balance\":3050.00"), response.body());
        assertTrue(response.body().contains("\"FOOD\":650.00"), response.body());
        assertTrue(response.body().contains("\"RENT\":1800.00"), response.body());
        assertTrue(response.body().contains("\"OTHER\":0.00"), response.body());
    }

    @Test
    void invalidIncomeReturnsValidationProblemDetails() throws IOException, InterruptedException {
        var response = post("/api/v1/finance/incomes", """
                {
                  "description": " ",
                  "amount": 0,
                  "transactionDate": null
                }
                """);

        assertEquals(400, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("")
                .startsWith("application/problem+json"));
        assertTrue(response.body().contains("\"title\":\"Validation failed\""), response.body());
        assertTrue(response.body().contains("\"description\""), response.body());
        assertTrue(response.body().contains("\"amount\""), response.body());
        assertTrue(response.body().contains("\"transactionDate\""), response.body());
    }

    @Test
    void invalidCategoryReturnsProblemDetails() throws IOException, InterruptedException {
        var response = post("/api/v1/finance/expenses", """
                {
                  "description": "Invalid expense",
                  "amount": 10.00,
                  "transactionDate": "2026-07-10",
                  "category": "INVALID"
                }
                """);

        assertEquals(400, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("")
                .startsWith("application/problem+json"));
        assertTrue(response.body().contains("\"title\":\"Invalid request\""), response.body());
    }

    @Test
    void categoriesReturnsAllSupportedValues() throws IOException, InterruptedException {
        var response = get("/api/v1/finance/categories");

        assertEquals(200, response.statusCode());
        assertEquals(
                "[\"FOOD\",\"TRANSPORT\",\"RENT\",\"LEISURE\",\"HEALTH\",\"OTHER\"]",
                response.body());
    }

    @Test
    void transactionListsSupportDateAndCategoryFilters() throws IOException, InterruptedException {
        post("/api/v1/finance/expenses", """
                {
                  "description": "July food",
                  "amount": 80.00,
                  "transactionDate": "2026-07-10",
                  "category": "FOOD"
                }
                """);
        post("/api/v1/finance/expenses", """
                {
                  "description": "August rent",
                  "amount": 1800.00,
                  "transactionDate": "2026-08-01",
                  "category": "RENT"
                }
                """);

        var response = get(
                "/api/v1/finance/expenses?from=2026-07-01&to=2026-07-31&category=FOOD");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("July food"), response.body());
        assertTrue(!response.body().contains("August rent"), response.body());
    }

    @Test
    void recurringRuleMaterializesDueOccurrence() throws IOException, InterruptedException {
        var today = LocalDate.now();
        var createResponse = post("/api/v1/finance/recurring-transactions", """
                {
                  "kind": "INCOME",
                  "description": "Recurring salary",
                  "amount": 5000.00,
                  "category": null,
                  "frequency": "MONTHLY",
                  "startDate": "%s",
                  "endDate": null
                }
                """.formatted(today));

        assertEquals(201, createResponse.statusCode());
        var listResponse = get("/api/v1/finance/incomes?from=" + today + "&to=" + today);
        assertEquals(200, listResponse.statusCode());
        assertTrue(listResponse.body().contains("Recurring salary"), listResponse.body());
        assertTrue(listResponse.body().contains("recurringTransactionId"), listResponse.body());
    }

    @Test
    void monthlyBudgetCombinesPlannedAndSpentAmounts() throws IOException, InterruptedException {
        var today = LocalDate.now();
        var month = YearMonth.from(today);
        post("/api/v1/finance/expenses", """
                {
                  "description": "Current food",
                  "amount": 125.00,
                  "transactionDate": "%s",
                  "category": "FOOD"
                }
                """.formatted(today));

        var setResponse = put(
                "/api/v1/finance/budgets/FOOD?month=" + month,
                """
                { "amount": 500.00 }
                """);

        assertEquals(200, setResponse.statusCode());
        assertTrue(setResponse.body().contains("\"planned\":500.00"), setResponse.body());
        assertTrue(setResponse.body().contains("\"spent\":125.00"), setResponse.body());
        assertTrue(setResponse.body().contains("\"remaining\":375.00"), setResponse.body());
    }

    @Test
    void openApiAndSwaggerAreAvailable() throws IOException, InterruptedException {
        var openApiResponse = get("/openapi/v1.json");
        var swaggerResponse = get("/swagger");

        assertEquals(200, openApiResponse.statusCode());
        assertTrue(openApiResponse.body().contains("\"openapi\""));
        assertTrue(openApiResponse.body().contains("/api/v1/finance/summary"));
        assertTrue(openApiResponse.body().contains("/api/v1/finance/incomes"));
        assertTrue(openApiResponse.body().contains("/api/v1/finance/expenses"));
        assertTrue(openApiResponse.body().contains("/api/v1/finance/categories"));
        assertEquals(200, swaggerResponse.statusCode());
        assertTrue(swaggerResponse.body().contains("Swagger UI"));
    }

    @Test
    void unknownResourceReturnsProblemDetails() throws IOException, InterruptedException {
        var response = get("/api/v1/finance/unknown");

        assertEquals(404, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("")
                .startsWith("application/problem+json"));
        assertTrue(response.body().contains("\"title\":\"Resource not found\""), response.body());
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return send("GET", path, null);
    }

    private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return send("POST", path, body);
    }

    private HttpResponse<String> put(String path, String body) throws IOException, InterruptedException {
        return send("PUT", path, body);
    }

    private HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        return send("DELETE", path, null);
    }

    private HttpResponse<String> send(String method, String path, String body)
            throws IOException, InterruptedException {
        return sendAsUser(method, path, body, DEMO_USER_ID);
    }

    private HttpResponse<String> sendAsUser(String method, String path, String body, String userId)
            throws IOException, InterruptedException {
        var requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .header("X-Finance-Control-User-Id", userId);

        if (body == null) {
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            requestBuilder
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }

        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String extractId(String body) {
        var matcher = ID_PATTERN.matcher(body);
        assertTrue(matcher.find(), body);
        return matcher.group(1);
    }
}
