import { useEffect, useMemo, useState } from 'react'
import { ApiError, api } from '../../api/client'
import type {
  Account,
  BudgetEntry,
  BudgetGrid as BudgetGridData,
  BudgetGridRow,
  Department,
  FieldError,
} from '../../api/types'
import { BudgetGrid } from './BudgetGrid'
import {
  type CellEdits,
  MONTH_LABELS,
  applyEdit,
  changedEntries,
  hasInvalidEdits,
} from './budgetEdits'

const YEARS = [2025, 2026, 2027]
const DEFAULT_YEAR = 2026

type Message =
  | { kind: 'success'; text: string }
  | { kind: 'error'; text: string; details: string[] }

export function BudgetEntryPage() {
  const [year, setYear] = useState(DEFAULT_YEAR)
  const [departmentCode, setDepartmentCode] = useState('')
  const [accounts, setAccounts] = useState<Account[]>([])
  const [departments, setDepartments] = useState<Department[]>([])
  const [grid, setGrid] = useState<BudgetGridData | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [addedRows, setAddedRows] = useState<BudgetGridRow[]>([])
  const [edits, setEdits] = useState<CellEdits>({})
  const [accountToAdd, setAccountToAdd] = useState('')
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<Message | null>(null)
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    Promise.all([api.listAccounts(), api.listDepartments()])
      .then(([accountList, departmentList]) => {
        setAccounts(accountList)
        setDepartments(departmentList)
      })
      .catch((error: unknown) => setLoadError(errorText(error)))
  }, [])

  useEffect(() => {
    let cancelled = false
    api
      .getBudgetGrid(year, departmentCode || undefined)
      .then((loaded) => {
        if (!cancelled) {
          setGrid(loaded)
          setLoadError(null)
        }
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setLoadError(errorText(error))
        }
      })
    // Ignore a slow response for a year/department the user has already moved away from.
    return () => {
      cancelled = true
    }
  }, [year, departmentCode, reloadToken])

  const loading =
    grid === null ||
    grid.year !== year ||
    grid.departmentCode !== (departmentCode || null)
  const rows = useMemo(
    () => [...(grid?.rows ?? []), ...addedRows],
    [grid, addedRows],
  )
  const entries = changedEntries(rows, edits)
  const invalid = hasInvalidEdits(rows, edits)
  const selectedDepartment = departments.find((d) => d.code === departmentCode)
  const addableAccounts = accounts.filter(
    (account) => !rows.some((row) => row.accountCode === account.code),
  )

  function resetEdits() {
    setEdits({})
    setAddedRows([])
    setMessage(null)
  }

  function changeFilter(nextYear: number, nextDepartment: string) {
    setYear(nextYear)
    setDepartmentCode(nextDepartment)
    setAccountToAdd('')
    resetEdits()
  }

  function addAccountRow() {
    const account = accounts.find((a) => a.code === accountToAdd)
    if (!account || !selectedDepartment) {
      return
    }
    setAddedRows((current) => [
      ...current,
      {
        accountCode: account.code,
        accountName: account.name,
        accountType: account.type,
        departmentCode: selectedDepartment.code,
        departmentName: selectedDepartment.name,
        months: Array<string | null>(12).fill(null),
        total: '0.00',
      },
    ])
    setAccountToAdd('')
  }

  async function save() {
    setSaving(true)
    setMessage(null)
    try {
      const result = await api.saveBudget(year, entries)
      setEdits({})
      setAddedRows([])
      setMessage({
        kind: 'success',
        text: `Saved ${entries.length} change${entries.length === 1 ? '' : 's'} (${result.created} new, ${result.updated} updated).`,
      })
      setReloadToken((token) => token + 1)
    } catch (error) {
      setMessage({
        kind: 'error',
        text: errorText(error),
        details: describeFieldErrors(error, entries, rows),
      })
    } finally {
      setSaving(false)
    }
  }

  return (
    <section aria-labelledby="budget-heading">
      <h2 id="budget-heading">Budget entry</h2>
      <p className="hint">
        Enter monthly budget amounts. Changed cells are highlighted until you
        save.
      </p>

      <div className="toolbar">
        <label>
          Fiscal year
          <select
            value={year}
            onChange={(e) =>
              changeFilter(Number(e.target.value), departmentCode)
            }
          >
            {YEARS.map((y) => (
              <option key={y} value={y}>
                FY{y}
              </option>
            ))}
          </select>
        </label>
        <label>
          Department
          <select
            value={departmentCode}
            onChange={(e) => changeFilter(year, e.target.value)}
          >
            <option value="">All departments</option>
            {departments.map((d) => (
              <option key={d.code} value={d.code}>
                {d.name}
              </option>
            ))}
          </select>
        </label>
        {selectedDepartment && (
          <div className="add-row">
            <label>
              Add account
              <select
                value={accountToAdd}
                onChange={(e) => setAccountToAdd(e.target.value)}
              >
                <option value="">Choose an account…</option>
                {addableAccounts.map((a) => (
                  <option key={a.code} value={a.code}>
                    {a.code} {a.name}
                  </option>
                ))}
              </select>
            </label>
            <button
              type="button"
              onClick={addAccountRow}
              disabled={!accountToAdd}
            >
              Add row
            </button>
          </div>
        )}
      </div>

      {loadError && (
        <p role="alert" className="message message-error">
          Could not load budgets: {loadError}
        </p>
      )}

      {loading && !loadError ? (
        <p>Loading…</p>
      ) : rows.length === 0 ? (
        <p>
          No budget for FY{year}
          {selectedDepartment ? ` in ${selectedDepartment.name}` : ''} yet.
          {selectedDepartment
            ? ' Add an account row to start.'
            : ' Choose a department to add rows.'}
        </p>
      ) : (
        <BudgetGrid
          rows={rows}
          edits={edits}
          showDepartment={!departmentCode}
          onCellChange={(row, monthIndex, input) =>
            setEdits((current) => applyEdit(current, row, monthIndex, input))
          }
        />
      )}

      <div className="actions">
        <button
          type="button"
          className="primary"
          onClick={save}
          disabled={saving || invalid || entries.length === 0}
        >
          {saving
            ? 'Saving…'
            : `Save changes${entries.length ? ` (${entries.length})` : ''}`}
        </button>
        <button
          type="button"
          onClick={resetEdits}
          disabled={
            saving ||
            (Object.keys(edits).length === 0 && addedRows.length === 0)
          }
        >
          Discard changes
        </button>
        {invalid && (
          <span className="field-error">
            Fix the highlighted cells before saving.
          </span>
        )}
      </div>

      {message?.kind === 'success' && (
        <p role="status" className="message message-success">
          {message.text}
        </p>
      )}
      {message?.kind === 'error' && (
        <div role="alert" className="message message-error">
          <p>{message.text}</p>
          {message.details.length > 0 && (
            <ul>
              {message.details.map((detail) => (
                <li key={detail}>{detail}</li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  )
}

function errorText(error: unknown): string {
  return error instanceof Error ? error.message : 'Unexpected error'
}

/** Turns "entries[3].amount: ..." from the server into "Marketing (Sales) – Apr: ..." for the user. */
function describeFieldErrors(
  error: unknown,
  sent: BudgetEntry[],
  rows: BudgetGridRow[],
): string[] {
  if (!(error instanceof ApiError) || !error.problem?.errors) {
    return []
  }
  return (error.problem.errors as FieldError[]).map(({ field, message }) => {
    const index = /^entries\[(\d+)]/.exec(field)?.[1]
    const entry = index === undefined ? undefined : sent[Number(index)]
    if (!entry) {
      return `${field}: ${message}`
    }
    const row = rows.find(
      (r) =>
        r.accountCode === entry.accountCode &&
        r.departmentCode === entry.departmentCode,
    )
    const label = row
      ? `${row.accountName} (${row.departmentName})`
      : `${entry.accountCode} (${entry.departmentCode})`
    return `${label} – ${MONTH_LABELS[entry.month - 1]}: ${message}`
  })
}
