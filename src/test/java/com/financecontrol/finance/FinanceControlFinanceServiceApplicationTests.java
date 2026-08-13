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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import com.financecontrol.finance.repository.ExpenseRepository;
import com.financecontrol.finance.repository.IncomeRepository;
import com.financecontrol.finance.repository.MonthlyBudgetRepository;
import com.financecontrol.finance.repository.RecurringTransactionRepository;
import com.financecontrol.finance.repository.FinancialGoalRepository;
import com.financecontrol.finance.repository.FinancialGoalContributionRepository;
import com.financecontrol.finance.repository.FinanceCategoryRepository;
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
    private static final Pattern NUMERIC_ID_PATTERN = Pattern.compile("\\\"id\\\":(\\d+)");
    private static final Pattern CATEGORY_CODE_PATTERN = Pattern.compile("\\\"code\\\":\\\"([^\\\"]+)\\\"");
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

    @Autowired
    private FinancialGoalRepository financialGoalRepository;

    @Autowired
    private FinancialGoalContributionRepository financialGoalContributionRepository;

    @Autowired
    private FinanceCategoryRepository financeCategoryRepository;

    @BeforeEach
    void cleanDatabase() {
        expenseRepository.deleteAll();
        incomeRepository.deleteAll();
        recurringTransactionRepository.deleteAll();
        monthlyBudgetRepository.deleteAll();
        financialGoalContributionRepository.deleteAll();
        financialGoalRepository.deleteAll();
        financeCategoryRepository.deleteAll();
    }

    @Test
    void financialGoalCrudCalculatesProgressAndStatus() throws IOException, InterruptedException {
        var incomeResponse = post("/api/v1/finance/incomes", """
                {
                  "description": "Monthly salary",
                  "amount": 8000.00,
                  "transactionDate": "%s"
                }
                """.formatted(LocalDate.now()));
        assertEquals(201, incomeResponse.statusCode());
        var incomeId = extractId(incomeResponse.body());

        var targetDate = LocalDate.now().plusMonths(3);
        var createResponse = post("/api/v1/finance/goals", """
                {
                  "name": "Emergency reserve",
                  "targetAmount": 10000.00,
                  "currentAmount": 2500.00,
                  "targetDate": "%s"
                }
                """.formatted(targetDate));

        assertEquals(201, createResponse.statusCode());
        var id = extractId(createResponse.body());
        assertTrue(createResponse.body().contains("\"progressPercentage\":25.00"), createResponse.body());
        assertTrue(createResponse.body().contains("\"status\":\"ACTIVE\""), createResponse.body());

        var foreignIncomeResponse = sendAsUser(
                "POST",
                "/api/v1/finance/incomes",
                """
                        {
                          "description": "Private income",
                          "amount": 100.00,
                          "transactionDate": "%s"
                        }
                        """.formatted(LocalDate.now()),
                "8750c27d-a3ff-4c8f-997b-c6f230005040");
        var foreignIncomeId = extractId(foreignIncomeResponse.body());
        var rejectedForeignSource = post(
                "/api/v1/finance/goals/" + id + "/contributions",
                """
                        {
                          "amount": 10.00,
                          "contributionDate": "%s",
                          "note": "Invalid source",
                          "sourceIncomeId": "%s"
                        }
                        """.formatted(LocalDate.now(), foreignIncomeId));
        assertEquals(404, rejectedForeignSource.statusCode());

        var contributionResponse = post(
                "/api/v1/finance/goals/" + id + "/contributions",
                """
                {
                  "amount": 7500.00,
                  "contributionDate": "%s",
                  "note": "Monthly savings",
                  "sourceIncomeId": "%s"
                }
                """.formatted(LocalDate.now(), incomeId));
        assertEquals(201, contributionResponse.statusCode());
        var contributionId = extractId(contributionResponse.body());
        assertTrue(contributionResponse.body().contains("\"amount\":7500.00"), contributionResponse.body());
        assertTrue(contributionResponse.body().contains("\"type\":\"CONTRIBUTION\""), contributionResponse.body());
        assertTrue(contributionResponse.body().contains("\"incomeId\":\"" + incomeId + "\""),
                contributionResponse.body());
        assertTrue(contributionResponse.body().contains("\"description\":\"Monthly salary\""),
                contributionResponse.body());

        var completedGoal = get("/api/v1/finance/goals/" + id);
        assertEquals(200, completedGoal.statusCode());
        assertTrue(completedGoal.body().contains("\"status\":\"COMPLETED\""), completedGoal.body());
        assertTrue(completedGoal.body().contains("\"remainingAmount\":0.00"), completedGoal.body());

        var history = get("/api/v1/finance/goals/" + id + "/contributions");
        assertEquals(200, history.statusCode());
        assertTrue(history.body().contains("\"type\":\"INITIAL\""), history.body());
        assertTrue(history.body().contains("\"type\":\"CONTRIBUTION\""), history.body());

        assertEquals(204, delete("/api/v1/finance/incomes/" + incomeId).statusCode());
        var historyAfterIncomeDeletion = get("/api/v1/finance/goals/" + id + "/contributions");
        assertTrue(historyAfterIncomeDeletion.body().contains("\"incomeId\":null"),
                historyAfterIncomeDeletion.body());
        assertTrue(historyAfterIncomeDeletion.body().contains("\"description\":\"Monthly salary\""),
                historyAfterIncomeDeletion.body());

        assertEquals(204, delete(
                "/api/v1/finance/goals/" + id + "/contributions/" + contributionId).statusCode());
        var activeGoal = get("/api/v1/finance/goals/" + id);
        assertTrue(activeGoal.body().contains("\"currentAmount\":2500.00"), activeGoal.body());
        assertTrue(activeGoal.body().contains("\"status\":\"ACTIVE\""), activeGoal.body());

        var invalidDirectUpdate = put("/api/v1/finance/goals/" + id, """
                {
                  "name": "Emergency reserve",
                  "targetAmount": 10000.00,
                  "currentAmount": 3000.00,
                  "targetDate": "%s"
                }
                """.formatted(targetDate));
        assertEquals(400, invalidDirectUpdate.statusCode());
        assertTrue(invalidDirectUpdate.body().contains(
                "Current amount can only be changed through goal contributions."),
                invalidDirectUpdate.body());

        assertTrue(get("/api/v1/finance/goals").body().contains(id));
        assertEquals(204, delete("/api/v1/finance/goals/" + id).statusCode());
        assertEquals(404, get("/api/v1/finance/goals/" + id).statusCode());
    }

    @Test
    void sourceIncomeAllocationCannotBeExceededEvenByConcurrentContributions()
            throws IOException, InterruptedException {
        var incomeResponse = post("/api/v1/finance/incomes", """
                {
                  "description": "Freelance project",
                  "amount": 500.00,
                  "transactionDate": "%s"
                }
                """.formatted(LocalDate.now()));
        var incomeId = extractId(incomeResponse.body());
        var firstGoalId = extractId(post("/api/v1/finance/goals", """
                {
                  "name": "First goal",
                  "targetAmount": 1000.00,
                  "currentAmount": 0.00,
                  "targetDate": "%s"
                }
                """.formatted(LocalDate.now().plusMonths(6))).body());
        var secondGoalId = extractId(post("/api/v1/finance/goals", """
                {
                  "name": "Second goal",
                  "targetAmount": 1000.00,
                  "currentAmount": 0.00,
                  "targetDate": "%s"
                }
                """.formatted(LocalDate.now().plusMonths(6))).body());
        var contributionBody = """
                {
                  "amount": 400.00,
                  "contributionDate": "%s",
                  "note": "Concurrent allocation",
                  "sourceIncomeId": "%s"
                }
                """.formatted(LocalDate.now(), incomeId);

        var firstContribution = postAsync(
                "/api/v1/finance/goals/" + firstGoalId + "/contributions",
                contributionBody);
        var secondContribution = postAsync(
                "/api/v1/finance/goals/" + secondGoalId + "/contributions",
                contributionBody);
        CompletableFuture.allOf(firstContribution, secondContribution).join();
        var statuses = List.of(
                firstContribution.join().statusCode(),
                secondContribution.join().statusCode());

        assertTrue(statuses.contains(201), statuses.toString());
        assertTrue(statuses.contains(400), statuses.toString());

        var incomeAfterAllocation = get("/api/v1/finance/incomes/" + incomeId);
        assertTrue(incomeAfterAllocation.body().contains("\"goalAllocatedAmount\":400.00"),
                incomeAfterAllocation.body());

        var allocationDetails = get(
                "/api/v1/finance/incomes/" + incomeId + "/goal-allocations");
        assertEquals(200, allocationDetails.statusCode());
        assertTrue(allocationDetails.body().contains("\"goalAllocatedAmount\":400.00"),
                allocationDetails.body());
        assertTrue(allocationDetails.body().contains("\"goalAvailableAmount\":100.00"),
                allocationDetails.body());
        assertTrue(allocationDetails.body().contains("\"amount\":400.00"),
                allocationDetails.body());
        assertTrue(
                allocationDetails.body().contains("First goal")
                        || allocationDetails.body().contains("Second goal"),
                allocationDetails.body());
        assertTrue(incomeAfterAllocation.body().contains("\"goalAvailableAmount\":100.00"),
                incomeAfterAllocation.body());

        var invalidIncomeReduction = put("/api/v1/finance/incomes/" + incomeId, """
                {
                  "description": "Freelance project",
                  "amount": 399.99,
                  "transactionDate": "%s"
                }
                """.formatted(LocalDate.now()));
        assertEquals(400, invalidIncomeReduction.statusCode());
        assertTrue(invalidIncomeReduction.body().contains(
                "Income amount cannot be lower than the amount already allocated to goals."),
                invalidIncomeReduction.body());
    }

    @Test
    void cashFlowProjectionIncludesRecordedAndFutureRecurringOccurrences()
            throws IOException, InterruptedException {
        var today = LocalDate.now();
        var recurringResponse = post("/api/v1/finance/recurring-transactions", """
                {
                  "kind": "INCOME",
                  "description": "Monthly income",
                  "amount": 100.00,
                  "category": null,
                  "frequency": "MONTHLY",
                  "startDate": "%s",
                  "endDate": null
                }
                """.formatted(today));
        assertEquals(201, recurringResponse.statusCode());

        var projection = get("/api/v1/finance/projections/cash-flow?months=2");

        assertEquals(200, projection.statusCode());
        assertTrue(projection.body().contains("\"months\":2"), projection.body());
        assertTrue(projection.body().contains("\"totalProjectedIncome\":200.00"), projection.body());
        assertTrue(projection.body().contains("\"projectedCumulativeBalance\":200.00"), projection.body());
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
        assertTrue(response.body().contains("\"title\":\"Validation failed\""), response.body());
    }

    @Test
    void categoriesReturnsAllSupportedValues() throws IOException, InterruptedException {
        var response = get("/api/v1/finance/categories");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"code\":\"FOOD\""), response.body());
        assertTrue(response.body().contains("\"name\":\"Alimentação\""), response.body());
        assertTrue(response.body().contains("\"code\":\"TRANSPORT\""), response.body());
        assertTrue(response.body().contains("\"code\":\"RENT\""), response.body());
        assertTrue(response.body().contains("\"code\":\"LEISURE\""), response.body());
        assertTrue(response.body().contains("\"code\":\"HEALTH\""), response.body());
        assertTrue(response.body().contains("\"code\":\"OTHER\""), response.body());
        assertTrue(response.body().contains("\"defaultCategory\":true"), response.body());
    }

    @Test
    void customCategoryCanBeRenamedAndOnlyDeletedWhenUnused()
            throws IOException, InterruptedException {
        var created = post("/api/v1/finance/categories", """
                { "name": "Academia" }
                """);
        assertEquals(201, created.statusCode());
        var categoryId = extract(created.body(), NUMERIC_ID_PATTERN);
        var categoryCode = extract(created.body(), CATEGORY_CODE_PATTERN);
        assertTrue(categoryCode.startsWith("CUSTOM_"), created.body());
        assertTrue(created.body().contains("\"defaultCategory\":false"), created.body());

        var expense = post("/api/v1/finance/expenses", """
                {
                  "description": "Mensalidade da academia",
                  "amount": 120.00,
                  "transactionDate": "2026-07-10",
                  "category": "%s"
                }
                """.formatted(categoryCode));
        assertEquals(201, expense.statusCode());
        var expenseId = extractId(expense.body());

        var renamed = put("/api/v1/finance/categories/" + categoryId, """
                { "name": "Saúde e academia" }
                """);
        assertEquals(200, renamed.statusCode());
        assertTrue(renamed.body().contains("Saúde e academia"), renamed.body());
        assertTrue(renamed.body().contains(categoryCode), renamed.body());

        var deleteInUse = delete("/api/v1/finance/categories/" + categoryId);
        assertEquals(400, deleteInUse.statusCode());
        assertTrue(deleteInUse.body().contains("Categories in use cannot be deleted"),
                deleteInUse.body());

        assertEquals(204, delete("/api/v1/finance/expenses/" + expenseId).statusCode());
        assertEquals(204, delete("/api/v1/finance/categories/" + categoryId).statusCode());
        assertTrue(!get("/api/v1/finance/categories").body().contains(categoryCode));
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
    void deletingRecurringRuleKeepsGeneratedTransactions() throws IOException, InterruptedException {
        var today = LocalDate.now();
        var description = "Income preserved after recurrence deletion";
        var createResponse = post("/api/v1/finance/recurring-transactions", """
                {
                  "kind": "INCOME",
                  "description": "%s",
                  "amount": 321.00,
                  "category": null,
                  "frequency": "MONTHLY",
                  "startDate": "%s",
                  "endDate": null
                }
                """.formatted(description, today));
        assertEquals(201, createResponse.statusCode());
        var recurringId = extractId(createResponse.body());

        var deleteResponse = delete("/api/v1/finance/recurring-transactions/" + recurringId);

        assertEquals(204, deleteResponse.statusCode(), deleteResponse.body());
        var recurringList = get("/api/v1/finance/recurring-transactions");
        assertEquals(200, recurringList.statusCode());
        assertTrue(!recurringList.body().contains(description), recurringList.body());
        var incomes = get("/api/v1/finance/incomes?from=" + today + "&to=" + today);
        assertEquals(200, incomes.statusCode());
        assertTrue(incomes.body().contains(description), incomes.body());
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
        assertTrue(setResponse.body().contains("\"name\":\"Alimentação\""), setResponse.body());
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
        assertTrue(openApiResponse.body().contains("/api/v1/finance/goals"));
        assertTrue(openApiResponse.body().contains("/api/v1/finance/projections/cash-flow"));
        assertEquals(200, swaggerResponse.statusCode());
        assertTrue(swaggerResponse.body().contains("Swagger UI"));
    }

    @Test
    void unknownResourceReturnsProblemDetails() throws IOException, InterruptedException {
        var correlationId = "61ec8ba6-c359-48c1-b2d7-f57ec7309369";
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/finance/unknown"))
                .header("X-Finance-Control-User-Id", DEMO_USER_ID)
                .header("X-Correlation-ID", correlationId)
                .GET()
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertEquals(correlationId, response.headers().firstValue("X-Correlation-ID").orElseThrow());
        assertTrue(response.headers().firstValue("content-type").orElse("")
                .startsWith("application/problem+json"));
        assertTrue(response.body().contains("\"title\":\"Resource not found\""), response.body());
        assertTrue(response.body().contains("\"correlationId\":\"" + correlationId + "\""), response.body());
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return send("GET", path, null);
    }

    private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return send("POST", path, body);
    }

    private CompletableFuture<HttpResponse<String>> postAsync(String path, String body) {
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .header("X-Finance-Control-User-Id", DEMO_USER_ID)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
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

    private static String extract(String body, Pattern pattern) {
        var matcher = pattern.matcher(body);
        assertTrue(matcher.find(), body);
        return matcher.group(1);
    }
}
