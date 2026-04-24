# Expense Tracker (Spring Boot + React)

Minimal full-stack expense tracker with production-minded behavior for retries, refreshes, and slow networks.

## Tech Stack

- Backend: Spring Boot + JDBC + H2 file database
- Frontend: React + Vite

## Project Structure

- `backend`: Spring Boot API
- `frontend`: React UI

## How to Run

### 1) Start backend

From `backend`:

```bash
./mvnw test
./mvnw spring-boot:run
```

Backend runs on `http://localhost:8080`.

### 2) Start frontend

From `frontend`:

```bash
npm install
npm run dev
```

Frontend runs on `http://localhost:5173`.

## API

### POST `/expenses`

Creates a new expense.

Request JSON:

```json
{
  "amount": 1250.5,
  "category": "Food",
  "description": "Groceries",
  "date": "2026-04-24"
}
```

Optional header:

- `Idempotency-Key`: if the same key is retried, API returns the same created expense instead of creating duplicates.

### GET `/expenses`

Returns list of expenses.

Optional query params:

- `category=<value>` for filtering
- `sort=date_desc` for newest date first

## Key Design Decisions

- **Persistence**: H2 file database (`jdbc:h2:file:...`) instead of in-memory so data survives browser refreshes and app restarts.
- **Money handling**: Java `BigDecimal` mapped to `DECIMAL(19,2)` for currency-safe arithmetic and storage.
- **Idempotency**: separate `idempotency_records` table maps `Idempotency-Key -> expense_id`, which protects against duplicate inserts during retries or repeated submits.
- **Resilience in UI**:
  - Submit button disables while request is in-flight.
  - Pending submission payload + idempotency key is stored in `sessionStorage`.
  - After refresh, user can retry safely using the same key.
  - Loading and error states are shown.

## Timebox Trade-offs

- Kept auth, pagination, and advanced analytics out to keep focus on correctness of create/list/filter/sort/total.
- Implemented basic integration tests on backend for idempotency and query behavior; broader test coverage (frontend tests/e2e) was deferred.
- Chose exact category match for filter for simplicity; could be expanded to case-insensitive and partial matching.

## Intentionally Not Done

- Deployment setup and live-hosted URL.
- Edit/delete expense flows.
- Category management and richer summary dashboards.
