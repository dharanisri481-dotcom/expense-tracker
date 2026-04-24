package com.finance.expensetracker.expense.repository;

import com.finance.expensetracker.expense.model.Expense;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ExpenseRepository {

    private static final RowMapper<Expense> EXPENSE_ROW_MAPPER = (resultSet, rowNum) -> new Expense(
            resultSet.getObject("id", UUID.class),
            resultSet.getBigDecimal("amount"),
            resultSet.getString("category"),
            resultSet.getString("description"),
            resultSet.getDate("date").toLocalDate(),
            resultSet.getTimestamp("created_at").toInstant()
    );

    private final JdbcTemplate jdbcTemplate;

    public ExpenseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Expense expense) {
        jdbcTemplate.update(
                "INSERT INTO expenses (id, amount, category, description, date, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                expense.id(),
                expense.amount(),
                expense.category(),
                expense.description(),
                Date.valueOf(expense.date()),
                Timestamp.from(expense.createdAt())
        );
    }

    public Optional<Expense> findById(UUID id) {
        return jdbcTemplate.query(
                "SELECT id, amount, category, description, date, created_at FROM expenses WHERE id = ?",
                EXPENSE_ROW_MAPPER,
                id
        ).stream().findFirst();
    }

    public List<Expense> findAll(Optional<String> category, boolean sortDateDesc) {
        StringBuilder sql = new StringBuilder("SELECT id, amount, category, description, date, created_at FROM expenses");
        List<Object> params = new ArrayList<>();

        category.filter(value -> !value.isBlank()).ifPresent(value -> {
            sql.append(" WHERE category = ?");
            params.add(value);
        });

        if (sortDateDesc) {
            sql.append(" ORDER BY date DESC, created_at DESC");
        } else {
            sql.append(" ORDER BY created_at DESC");
        }

        return jdbcTemplate.query(sql.toString(), EXPENSE_ROW_MAPPER, params.toArray());
    }
}
