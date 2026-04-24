package com.finance.expensetracker.expense.api;

import com.finance.expensetracker.expense.model.Expense;
import com.finance.expensetracker.expense.service.CreateExpenseResult;
import com.finance.expensetracker.expense.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody CreateExpenseRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader
    ) {
        String idempotencyKey = (idempotencyKeyHeader == null || idempotencyKeyHeader.isBlank())
                ? UUID.randomUUID().toString()
                : idempotencyKeyHeader;

        CreateExpenseResult result = expenseService.createExpense(request, idempotencyKey);
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(ExpenseResponse.from(result.expense()));
    }

    @GetMapping
    public List<ExpenseResponse> getExpenses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort
    ) {
        List<Expense> expenses = expenseService.getExpenses(Optional.ofNullable(category), sort);
        return expenses.stream().map(ExpenseResponse::from).toList();
    }
}
