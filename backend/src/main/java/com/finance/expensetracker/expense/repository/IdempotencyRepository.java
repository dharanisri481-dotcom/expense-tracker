package com.finance.expensetracker.expense.repository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class IdempotencyRepository {

    private final JdbcTemplate jdbcTemplate;

    public IdempotencyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UUID> findExpenseIdByKey(String idempotencyKey) {
        return jdbcTemplate.query(
                "SELECT expense_id FROM idempotency_records WHERE idempotency_key = ?",
                (resultSet, rowNum) -> resultSet.getObject("expense_id", UUID.class),
                idempotencyKey
        ).stream().findFirst();
    }

    public boolean save(String idempotencyKey, UUID expenseId) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO idempotency_records (idempotency_key, expense_id, created_at) VALUES (?, ?, ?)",
                    idempotencyKey,
                    expenseId,
                    Timestamp.from(Instant.now())
            );
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }
}
