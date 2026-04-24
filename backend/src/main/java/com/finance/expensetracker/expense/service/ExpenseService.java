package com.finance.expensetracker.expense.service;

import com.finance.expensetracker.expense.api.CreateExpenseRequest;
import com.finance.expensetracker.expense.model.Expense;
import com.finance.expensetracker.expense.repository.ExpenseRepository;
import com.finance.expensetracker.expense.repository.IdempotencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final IdempotencyRepository idempotencyRepository;

    public ExpenseService(ExpenseRepository expenseRepository, IdempotencyRepository idempotencyRepository) {
        this.expenseRepository = expenseRepository;
        this.idempotencyRepository = idempotencyRepository;
    }

    @Transactional
    public CreateExpenseResult createExpense(CreateExpenseRequest request, String idempotencyKey) {
        Optional<UUID> existingExpenseId = idempotencyRepository.findExpenseIdByKey(idempotencyKey);
        if (existingExpenseId.isPresent()) {
            Expense existingExpense = expenseRepository.findById(existingExpenseId.get())
                    .orElseThrow(() -> new IllegalStateException("Idempotency key points to missing expense."));
            return new CreateExpenseResult(existingExpense, true);
        }

        Expense expense = new Expense(
                UUID.randomUUID(),
                request.amount(),
                request.category().trim(),
                request.description().trim(),
                request.date(),
                Instant.now()
        );
        expenseRepository.save(expense);

        boolean inserted = idempotencyRepository.save(idempotencyKey, expense.id());
        if (!inserted) {
            UUID expenseId = idempotencyRepository.findExpenseIdByKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Idempotency conflict without mapping."));
            Expense existingExpense = expenseRepository.findById(expenseId)
                    .orElseThrow(() -> new IllegalStateException("Idempotency key points to missing expense."));
            return new CreateExpenseResult(existingExpense, true);
        }

        return new CreateExpenseResult(expense, false);
    }

    public List<Expense> getExpenses(Optional<String> category, String sort) {
        boolean sortDateDesc = "date_desc".equalsIgnoreCase(sort);
        return expenseRepository.findAll(category, sortDateDesc);
    }
}
