CREATE TABLE IF NOT EXISTS expenses (
    id UUID PRIMARY KEY,
    amount DECIMAL(19, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS idempotency_records (
    idempotency_key VARCHAR(120) PRIMARY KEY,
    expense_id UUID NOT NULL REFERENCES expenses(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
