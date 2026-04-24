package com.finance.expensetracker.expense.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Expense(
        UUID id,
        BigDecimal amount,
        String category,
        String description,
        LocalDate date,
        Instant createdAt
) {
}
