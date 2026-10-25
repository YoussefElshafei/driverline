import { Fragment } from 'react'
import type {
  ForecastMethod,
  ForecastReport,
  OutlookLine,
} from '../../api/types'
import { formatPercent, formatSignedWholeDollars } from '../../lib/format'
import { formatWholeDollars } from '../../lib/money'
import { MONTH_SHORT } from '../../lib/months'
import { FavorabilityBadge } from '../variance/FavorabilityBadge'

const METHOD_LABELS: Record<ForecastMethod, string> = {
  UNITS_X_RATE: 'Units × rate',
  PERCENT_OF_REVENUE: '% of revenue',
  BUDGET: 'Budget',
}

function OutlookCells({ line }: { line: OutlookLine }) {
  return (
    <>
      <td className="num">
        {formatWholeDollars(line.outlook)}
        <span className="cell-subline">
          budget {formatWholeDollars(line.budget)}
        </span>
      </td>
      <td className="num">
        {formatSignedWholeDollars(line.variance)}
        <span className="cell-subline">
          {formatPercent(line.variancePercent)}
        </span>
      </td>
      <td>
        <FavorabilityBadge favorability={line.favorability} />
      </td>
    </>
  )
}

/**
 * Twelve months per line (forecast months shaded), then the full-year outlook with its budget beneath, and the
 * variance. Whole dollars keep 16 columns readable; the scenario cards and the Excel export show exact cents.
 */
export function ForecastTable({ report }: { report: ForecastReport }) {
  return (
    <div className="table-scroll">
      <table className="report forecast-table">
        <thead>
          <tr>
            <th scope="col" className="sticky-col">
              Department / account
            </th>
            {MONTH_SHORT.map((month, index) => {
              const forecast = index + 1 > report.latestActualMonth
              return (
                <th
                  key={month}
                  scope="col"
                  className={`num${forecast ? ' cell-forecast' : ''}`}
                >
                  {month}
                  <span className="month-kind">{forecast ? ' F' : ' A'}</span>
                </th>
              )
            })}
            <th scope="col" className="num">
              Full year
            </th>
            <th scope="col" className="num">
              Variance
            </th>
            <th scope="col">Status</th>
          </tr>
        </thead>
        <tbody>
          {report.departments.map((department) => (
            <Fragment key={department.departmentCode}>
              <tr className="row-department">
                <th scope="row" colSpan={13} className="sticky-col">
                  {department.departmentName}
                  <span className="hint"> · net contribution</span>
                </th>
                <OutlookCells line={department.netContribution} />
              </tr>
              {department.lines.map((line) => (
                <tr key={line.accountCode} className="row-account">
                  <th scope="row" className="sticky-col">
                    {line.accountCode} {line.accountName}
                    <span className="method-tag">
                      {METHOD_LABELS[line.method]}
                    </span>
                  </th>
                  {line.months.map((month) => (
                    <td
                      key={month.month}
                      className={`num${month.actual ? '' : ' cell-forecast'}`}
                      data-testid={`${department.departmentCode}-${line.accountCode}-${month.month}`}
                    >
                      {formatWholeDollars(month.amount)}
                    </td>
                  ))}
                  <OutlookCells line={line.outlook} />
                </tr>
              ))}
            </Fragment>
          ))}
        </tbody>
        <tfoot>
          <tr className="row-subtotal">
            <th scope="row" colSpan={13}>
              Total revenue
            </th>
            <OutlookCells line={report.summary.revenue} />
          </tr>
          <tr className="row-subtotal">
            <th scope="row" colSpan={13}>
              Total expenses
            </th>
            <OutlookCells line={report.summary.expenses} />
          </tr>
          <tr className="row-total">
            <th scope="row" colSpan={13}>
              Net income
            </th>
            <OutlookCells line={report.summary.netIncome} />
          </tr>
        </tfoot>
      </table>
    </div>
  )
}
