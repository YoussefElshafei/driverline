import type { BudgetGridRow } from '../../api/types'
import { formatMoney } from '../../lib/money'
import {
  type CellEdits,
  MONTH_LABELS,
  cellError,
  cellKey,
  displayValue,
  rowTotal,
} from './budgetEdits'

interface BudgetGridProps {
  rows: BudgetGridRow[]
  edits: CellEdits
  showDepartment: boolean
  onCellChange: (row: BudgetGridRow, monthIndex: number, input: string) => void
}

export function BudgetGrid({
  rows,
  edits,
  showDepartment,
  onCellChange,
}: BudgetGridProps) {
  return (
    <div className="table-scroll">
      <table className="grid">
        <thead>
          <tr>
            <th scope="col" className="sticky-col">
              Account
            </th>
            {showDepartment && <th scope="col">Department</th>}
            {MONTH_LABELS.map((month) => (
              <th key={month} scope="col" className="num">
                {month}
              </th>
            ))}
            <th scope="col" className="num">
              Total
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={`${row.departmentCode}|${row.accountCode}`}>
              <th scope="row" className="sticky-col">
                {row.accountCode} {row.accountName}
                <span
                  className={`badge badge-${row.accountType.toLowerCase()}`}
                >
                  {row.accountType === 'REVENUE' ? 'Revenue' : 'Expense'}
                </span>
              </th>
              {showDepartment && <td>{row.departmentName}</td>}
              {MONTH_LABELS.map((month, monthIndex) => {
                const error = cellError(row, monthIndex, edits)
                const edited = cellKey(row, monthIndex) in edits
                return (
                  <td key={month}>
                    <input
                      className={`cell${edited ? ' cell-edited' : ''}`}
                      inputMode="decimal"
                      aria-label={`${row.accountName}, ${row.departmentName}, ${month}`}
                      aria-invalid={error !== null}
                      title={error ?? undefined}
                      value={displayValue(row, monthIndex, edits)}
                      onChange={(event) =>
                        onCellChange(row, monthIndex, event.target.value)
                      }
                    />
                  </td>
                )
              })}
              <td
                className="num total"
                data-testid={`total-${row.departmentCode}-${row.accountCode}`}
              >
                {formatMoney(rowTotal(row, edits))}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
