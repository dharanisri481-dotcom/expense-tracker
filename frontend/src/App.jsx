import { useCallback, useEffect, useMemo, useState } from 'react'
import './App.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const PENDING_SUBMISSION_KEY = 'expenseTracker.pendingSubmission'

const emptyForm = {
  amount: '',
  category: '',
  description: '',
  date: '',
}

function buildQuery(filterCategory, sortDateDesc) {
  const params = new URLSearchParams()
  if (filterCategory) {
    params.set('category', filterCategory)
  }
  if (sortDateDesc) {
    params.set('sort', 'date_desc')
  }
  const query = params.toString()
  return query ? `?${query}` : ''
}

function App() {
  const [form, setForm] = useState(emptyForm)
  const [expenses, setExpenses] = useState([])
  const [filterCategory, setFilterCategory] = useState('')
  const [sortDateDesc, setSortDateDesc] = useState(true)
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [pendingSubmission, setPendingSubmission] = useState(() => {
    const raw = window.sessionStorage.getItem(PENDING_SUBMISSION_KEY)
    if (!raw) {
      return null
    }
    try {
      return JSON.parse(raw)
    } catch {
      window.sessionStorage.removeItem(PENDING_SUBMISSION_KEY)
      return null
    }
  })

  const categories = useMemo(() => {
    const values = [...new Set(expenses.map((expense) => expense.category))]
    return values.sort((a, b) => a.localeCompare(b))
  }, [expenses])

  const totalAmount = useMemo(() => {
    const total = expenses.reduce((sum, expense) => sum + Number(expense.amount), 0)
    return total.toFixed(2)
  }, [expenses])

  const fetchExpenses = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const query = buildQuery(filterCategory, sortDateDesc)
      const response = await fetch(`${API_BASE_URL}/expenses${query}`)
      if (!response.ok) {
        throw new Error('Could not fetch expenses.')
      }
      const data = await response.json()
      setExpenses(data)
    } catch (fetchError) {
      setError(fetchError.message)
    } finally {
      setLoading(false)
    }
  }, [filterCategory, sortDateDesc])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void fetchExpenses()
    }, 0)
    return () => window.clearTimeout(timer)
  }, [fetchExpenses])

  function updateForm(field, value) {
    setForm((previous) => ({
      ...previous,
      [field]: value,
    }))
  }

  async function submitExpense(payload, idempotencyKey) {
    const response = await fetch(`${API_BASE_URL}/expenses`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey,
      },
      body: JSON.stringify(payload),
    })

    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({}))
      const message = errorBody.message ?? 'Failed to save expense.'
      throw new Error(message)
    }
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError('')

    const payload = {
      amount: Number.parseFloat(form.amount),
      category: form.category.trim(),
      description: form.description.trim(),
      date: form.date,
    }

    const alreadyPending =
      pendingSubmission &&
      JSON.stringify(pendingSubmission.payload) === JSON.stringify(payload)
    const idempotencyKey = alreadyPending ? pendingSubmission.idempotencyKey : crypto.randomUUID()
    const newPendingSubmission = { payload, idempotencyKey }

    window.sessionStorage.setItem(
      PENDING_SUBMISSION_KEY,
      JSON.stringify(newPendingSubmission),
    )
    setPendingSubmission(newPendingSubmission)

    try {
      await submitExpense(payload, idempotencyKey)
      window.sessionStorage.removeItem(PENDING_SUBMISSION_KEY)
      setPendingSubmission(null)
      setForm(emptyForm)
      await fetchExpenses()
    } catch (submitError) {
      setError(submitError.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function retryPendingSubmission() {
    if (!pendingSubmission) {
      return
    }
    setSubmitting(true)
    setError('')
    try {
      await submitExpense(pendingSubmission.payload, pendingSubmission.idempotencyKey)
      window.sessionStorage.removeItem(PENDING_SUBMISSION_KEY)
      setPendingSubmission(null)
      setForm(emptyForm)
      await fetchExpenses()
    } catch (retryError) {
      setError(retryError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="container">
      <h1>Expense Tracker</h1>

      {pendingSubmission ? (
        <div className="status warning">
          A previous submission may still be pending.
          <button
            type="button"
            onClick={retryPendingSubmission}
            disabled={submitting}
            className="inline-button"
          >
            Retry now
          </button>
        </div>
      ) : null}

      {error ? (
        <div className="status error">
          {error}
          <button
            type="button"
            onClick={() => void fetchExpenses()}
            className="inline-button"
            disabled={loading}
          >
            Retry
          </button>
        </div>
      ) : null}

      <section className="card">
        <h2>Add Expense</h2>
        <form onSubmit={handleSubmit} className="form-grid">
          <label>
            Amount (INR)
            <input
              type="number"
              min="0.01"
              step="0.01"
              required
              value={form.amount}
              onChange={(event) => updateForm('amount', event.target.value)}
            />
          </label>

          <label>
            Category
            <input
              type="text"
              required
              value={form.category}
              onChange={(event) => updateForm('category', event.target.value)}
            />
          </label>

          <label>
            Date
            <input
              type="date"
              required
              value={form.date}
              onChange={(event) => updateForm('date', event.target.value)}
            />
          </label>

          <label className="full-width">
            Description
            <input
              type="text"
              required
              value={form.description}
              onChange={(event) => updateForm('description', event.target.value)}
            />
          </label>

          <button type="submit" disabled={submitting}>
            {submitting ? 'Saving...' : 'Save Expense'}
          </button>
        </form>
      </section>

      <section className="card">
        <div className="controls">
          <label>
            Filter by Category
            <select
              value={filterCategory}
              onChange={(event) => setFilterCategory(event.target.value)}
            >
              <option value="">All categories</option>
              {categories.map((category) => (
                <option key={category} value={category}>
                  {category}
                </option>
              ))}
            </select>
          </label>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={sortDateDesc}
              onChange={(event) => setSortDateDesc(event.target.checked)}
            />
            Sort by date (newest first)
          </label>
        </div>

        <p className="total">Total: ₹{totalAmount}</p>

        {loading ? (
          <p>Loading expenses...</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Category</th>
                <th>Description</th>
                <th>Amount (₹)</th>
              </tr>
            </thead>
            <tbody>
              {expenses.length === 0 ? (
                <tr>
                  <td colSpan="4">No expenses found.</td>
                </tr>
              ) : (
                expenses.map((expense) => (
                  <tr key={expense.id}>
                    <td>{expense.date}</td>
                    <td>{expense.category}</td>
                    <td>{expense.description}</td>
                    <td>{Number(expense.amount).toFixed(2)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </section>
    </main>
  )
}

export default App
