package com.finance.expensetracker.expense.api;

import com.finance.expensetracker.expense.model.Expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        BigDecimal amount,
        String category,
        String description,
        LocalDate date,
        Instant createdAt
) {
    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.id(),
                expense.amount(),
                expense.category(),
                expense.description(),
                expense.date(),
                expense.createdAt()
        );
    }
}
