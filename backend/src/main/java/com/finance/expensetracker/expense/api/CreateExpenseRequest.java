package com.finance.expensetracker.expense.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,
        @NotBlank(message = "Category is required")
        String category,
        @NotBlank(message = "Description is required")
        String description,
        @NotNull(message = "Date is required")
        LocalDate date
) {
}
