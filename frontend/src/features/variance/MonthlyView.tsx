import type {
  Account,
  Department,
  MonthlyVarianceReport,
} from '../../api/types'
import { formatMoney } from '../../lib/money'
import { monthName } from '../../lib/months'
import { VarianceCells, VarianceHeaderCells } from './VarianceCells'

export interface MonthlyScope {
  department?: string
  account?: string
}

interface MonthlyViewProps {
  report: MonthlyVarianceReport
  scope: MonthlyScope
  accounts: Account[]
  departments: Department[]
  onScopeChange: (scope: MonthlyScope) => void
}

/** A 12-month trend. Months without actuals show their budget only. */
export function MonthlyView({
  report,
  scope,
  accounts,
  departments,
  onScopeChange,
}: MonthlyViewProps) {
  return (
    <>
      <div className="toolbar">
        <label>
          Department
          <select
            value={scope.department ?? ''}
            onChange={(e) =>
              onScopeChange({
                ...scope,
                department: e.target.value || undefined,
              })
            }
          >
            <option value="">All departments</option>
            {departments.map((d) => (
              <option key={d.code} value={d.code}>
                {d.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Account
          <select
            value={scope.account ?? ''}
            onChange={(e) =>
              onScopeChange({ ...scope, account: e.target.value || undefined })
            }
          >
            <option value="">Net income (all accounts)</option>
            {accounts.map((a) => (
              <option key={a.code} value={a.code}>
                {a.code} {a.name}
              </option>
            ))}
          </select>
        </label>
      </div>
      <h3 className="scope-title">{report.scope}</h3>
      <div className="table-scroll">
        <table className="report">
          <thead>
            <tr>
              <th scope="col">Month</th>
              <VarianceHeaderCells />
            </tr>
          </thead>
          <tbody>
            {report.months.map((row) => (
              <tr
                key={row.month}
                className={row.closed ? undefined : 'row-open'}
              >
                <th scope="row">{monthName(row.month)}</th>
                {row.closed && row.variance ? (
                  <VarianceCells line={row.variance} />
                ) : (
                  <>
                    <td className="num">{formatMoney(row.budget)}</td>
                    <td className="num">—</td>
                    <td className="num">—</td>
                    <td className="num">—</td>
                    <td>
                      <span className="flag flag-open">Not closed</span>
                    </td>
                  </>
                )}
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="row-total">
              <th scope="row">Closed months total</th>
              <VarianceCells line={report.closedMonthsTotal} />
            </tr>
          </tfoot>
        </table>
      </div>
    </>
  )
}
