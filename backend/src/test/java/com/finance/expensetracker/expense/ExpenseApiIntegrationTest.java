package com.finance.expensetracker.expense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExpenseApiIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM idempotency_records");
        jdbcTemplate.update("DELETE FROM expenses");
    }

    @Test
    void postWithSameIdempotencyKeyCreatesSingleExpense() throws Exception {
        String payload = """
                {
                    "amount": 350.50,
                    "category": "Food",
                    "description": "Groceries",
                    "date": "2026-04-24"
                }
                """;

        ResponseData firstResponse = postExpense("same-key", payload);
        ResponseData secondResponse = postExpense("same-key", payload);
        assertThat(firstResponse.statusCode()).isEqualTo(201);
        assertThat(secondResponse.statusCode()).isEqualTo(200);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM expenses", Integer.class)).isEqualTo(1);
        assertThat(extractId(secondResponse.body())).isEqualTo(extractId(firstResponse.body()));
    }

    @Test
    void getSupportsCategoryAndDateDescSort() throws Exception {
        createExpense("k1", "Travel", "Cab", "2026-04-20");
        createExpense("k2", "Travel", "Flight", "2026-04-24");
        createExpense("k3", "Food", "Lunch", "2026-04-22");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/expenses?category=Travel&sort=date_desc"))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Flight").contains("Cab");
        assertThat(response.body().indexOf("Flight")).isLessThan(response.body().indexOf("Cab"));
    }

    private void createExpense(String key, String category, String description, String date) throws Exception {
        String payload = """
                {
                    "amount": %s,
                    "category": "%s",
                    "description": "%s",
                    "date": "%s"
                }
                """.formatted(BigDecimal.valueOf(100), category, description, date);
        ResponseData response = postExpense(key, payload);
        assertThat(response.statusCode()).isEqualTo(201);
    }

    private ResponseData postExpense(
            String key,
            String payload
    ) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/expenses"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", key)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new ResponseData(response.statusCode(), response.body());
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String extractId(String json) {
        int idStart = json.indexOf("\"id\":\"");
        if (idStart < 0) {
            return "";
        }
        int valueStart = idStart + 6;
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueEnd < 0) {
            return "";
        }
        return json.substring(valueStart, valueEnd);
    }

    private record ResponseData(int statusCode, String body) {
    }
}
