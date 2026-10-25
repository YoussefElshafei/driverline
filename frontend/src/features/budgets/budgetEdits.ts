import type { BudgetEntry, BudgetGridRow } from '../../api/types'
import { formatAmount, parseMoney, sumAmounts, toCents } from '../../lib/money'
import { MONTH_SHORT } from '../../lib/months'

/** What the user has typed, keyed by cell. Cells the user hasn't touched are absent. */
export type CellEdits = Readonly<Record<string, string>>

/** Month column headings, January first. */
export const MONTH_LABELS = MONTH_SHORT

export function cellKey(row: BudgetGridRow, monthIndex: number): string {
  return `${row.departmentCode}|${row.accountCode}|${monthIndex}`
}

/** The text to show in a cell: the user's edit if there is one, otherwise the saved amount with separators. */
export function displayValue(
  row: BudgetGridRow,
  monthIndex: number,
  edits: CellEdits,
): string {
  const saved = row.months[monthIndex]
  return (
    edits[cellKey(row, monthIndex)] ??
    (saved === null ? '' : formatAmount(saved))
  )
}

/** Records an edit, or forgets it when the user types the original value back. */
export function applyEdit(
  edits: CellEdits,
  row: BudgetGridRow,
  monthIndex: number,
  input: string,
): CellEdits {
  const key = cellKey(row, monthIndex)
  const { [key]: _previous, ...rest } = edits
  void _previous
  const original = row.months[monthIndex]
  const unchanged =
    original === null
      ? input.trim() === ''
      : parseMoney(input).ok && parseAmount(input) === original
  return unchanged ? rest : { ...rest, [key]: input }
}

export function cellError(
  row: BudgetGridRow,
  monthIndex: number,
  edits: CellEdits,
): string | null {
  const input = edits[cellKey(row, monthIndex)]
  if (input === undefined) {
    return null
  }
  const result = parseMoney(input)
  return result.ok ? null : result.error
}

/** Row total using valid edits; cells with invalid input still count at their saved amount. */
export function rowTotal(row: BudgetGridRow, edits: CellEdits): string {
  return sumAmounts(
    row.months.map((saved, monthIndex) => {
      const input = edits[cellKey(row, monthIndex)]
      if (input === undefined) {
        return saved
      }
      const result = parseMoney(input)
      return result.ok ? result.amount : saved
    }),
  )
}

export function hasInvalidEdits(
  rows: BudgetGridRow[],
  edits: CellEdits,
): boolean {
  return rows.some((row) =>
    row.months.some((_, monthIndex) => cellError(row, monthIndex, edits)),
  )
}

/** The entries to send to PUT /api/budgets: every valid edited cell whose value actually changed. */
export function changedEntries(
  rows: BudgetGridRow[],
  edits: CellEdits,
): BudgetEntry[] {
  return rows.flatMap((row) =>
    row.months.flatMap((saved, monthIndex) => {
      const input = edits[cellKey(row, monthIndex)]
      const result = input === undefined ? null : parseMoney(input)
      if (!result?.ok) {
        return []
      }
      if (saved !== null && toCents(saved) === toCents(result.amount)) {
        return []
      }
      return [
        {
          accountCode: row.accountCode,
          departmentCode: row.departmentCode,
          month: monthIndex + 1,
          amount: result.amount,
        },
      ]
    }),
  )
}

function parseAmount(input: string): string | null {
  const result = parseMoney(input)
  return result.ok ? result.amount : null
}
