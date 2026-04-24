package com.finance.expensetracker.expense.service;

import com.finance.expensetracker.expense.model.Expense;

public record CreateExpenseResult(Expense expense, boolean replayed) {
}
