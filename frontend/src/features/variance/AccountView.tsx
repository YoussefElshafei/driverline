import type {
  AccountVarianceReport,
  AccountVarianceRow,
  VarianceLine,
} from '../../api/types'
import { SummaryCards } from './SummaryCards'
import { VarianceCells, VarianceHeaderCells } from './VarianceCells'

interface AccountViewProps {
  report: AccountVarianceReport
  onShowTrend: (accountCode: string) => void
}

/** Profit-and-loss layout: revenue accounts, expense accounts, net income. */
export function AccountView({ report, onShowTrend }: AccountViewProps) {
  return (
    <>
      <SummaryCards summary={report.summary} />
      <p className="hint">Each account is combined across all departments.</p>
      <div className="table-scroll">
        <table className="report">
          <thead>
            <tr>
              <th scope="col">Account</th>
              <VarianceHeaderCells />
              <th scope="col">
                <span className="visually-hidden">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <Section
              title="Revenue"
              rows={report.revenue}
              total={report.summary.revenue}
              onShowTrend={onShowTrend}
            />
            <Section
              title="Expenses"
              rows={report.expenses}
              total={report.summary.expenses}
              onShowTrend={onShowTrend}
            />
          </tbody>
          <tfoot>
            <tr className="row-total">
              <th scope="row">Net income</th>
              <VarianceCells line={report.summary.netIncome} />
              <td />
            </tr>
          </tfoot>
        </table>
      </div>
    </>
  )
}

function Section({
  title,
  rows,
  total,
  onShowTrend,
}: {
  title: string
  rows: AccountVarianceRow[]
  total: VarianceLine
  onShowTrend: (accountCode: string) => void
}) {
  return (
    <>
      <tr className="row-section">
        <th scope="rowgroup" colSpan={7}>
          {title}
        </th>
      </tr>
      {rows.map((row) => (
        <tr key={row.accountCode} className="row-account">
          <th scope="row">
            {row.accountCode} {row.accountName}
          </th>
          <VarianceCells line={row.line} />
          <td>
            <button
              type="button"
              className="link-button"
              aria-label={`Monthly trend for ${row.accountCode} ${row.accountName}`}
              onClick={() => onShowTrend(row.accountCode)}
            >
              Trend
            </button>
          </td>
        </tr>
      ))}
      <tr className="row-subtotal">
        <th scope="row">Total {title.toLowerCase()}</th>
        <VarianceCells line={total} />
        <td />
      </tr>
    </>
  )
}
