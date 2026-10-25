import { useEffect, useState } from 'react'
import {
  type MonthlyQuery,
  type PeriodQuery,
  api,
  varianceApi,
  varianceExportUrl,
} from '../../api/client'
import type {
  Account,
  AccountVarianceReport,
  Department,
  DepartmentVarianceReport,
  MonthlyVarianceReport,
} from '../../api/types'
import { MONTH_LONG, monthName } from '../../lib/months'
import { AccountView } from './AccountView'
import { CommentaryPanel } from './CommentaryPanel'
import { DepartmentView } from './DepartmentView'
import { type MonthlyScope, MonthlyView } from './MonthlyView'

type View = 'department' | 'account' | 'month'

type LoadedReport =
  | { view: 'department'; key: string; data: DepartmentVarianceReport }
  | { view: 'account'; key: string; data: AccountVarianceReport }
  | { view: 'month'; key: string; data: MonthlyVarianceReport }

const YEARS = [2025, 2026, 2027]
const DEFAULT_YEAR = 2026
const VIEWS: { id: View; label: string }[] = [
  { id: 'department', label: 'By department' },
  { id: 'account', label: 'By account' },
  { id: 'month', label: 'By month' },
]

export function VariancePage() {
  const [year, setYear] = useState(DEFAULT_YEAR)
  // Undefined months mean "let the server choose": January through the latest month with actuals.
  const [fromMonth, setFromMonth] = useState<number | undefined>()
  const [toMonth, setToMonth] = useState<number | undefined>()
  const [view, setView] = useState<View>('department')
  const [monthlyScope, setMonthlyScope] = useState<MonthlyScope>({})
  const [accounts, setAccounts] = useState<Account[]>([])
  const [departments, setDepartments] = useState<Department[]>([])
  const [report, setReport] = useState<LoadedReport | null>(null)
  const [error, setError] = useState<string | null>(null)

  const periodQuery: PeriodQuery = { year, fromMonth, toMonth }
  const monthlyQuery: MonthlyQuery = { year, ...monthlyScope }
  const requestKey = JSON.stringify(
    view === 'month' ? { view, ...monthlyQuery } : { view, ...periodQuery },
  )

  useEffect(() => {
    Promise.all([api.listAccounts(), api.listDepartments()])
      .then(([accountList, departmentList]) => {
        setAccounts(accountList)
        setDepartments(departmentList)
      })
      .catch(() => {
        // The report still works without the filter lists; the error shows if the report fails too.
      })
  }, [])

  useEffect(() => {
    let cancelled = false
    // The key doubles as the request: it changes exactly when a new report is needed.
    // `view` picks the endpoint and must not be sent as a query parameter.
    const { view: requestedView, ...query } = JSON.parse(requestKey) as {
      view: View
    } & PeriodQuery &
      MonthlyQuery
    const load: Promise<LoadedReport> =
      requestedView === 'department'
        ? varianceApi
            .byDepartment(query)
            .then((data) => ({ view: 'department', key: requestKey, data }))
        : requestedView === 'account'
          ? varianceApi
              .byAccount(query)
              .then((data) => ({ view: 'account', key: requestKey, data }))
          : varianceApi
              .byMonth(query)
              .then((data) => ({ view: 'month', key: requestKey, data }))
    load
      .then((loaded) => {
        if (!cancelled) {
          setReport(loaded)
          setError(null)
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Unexpected error')
        }
      })
    return () => {
      cancelled = true
    }
  }, [requestKey])

  const loading = report?.key !== requestKey && error === null
  // The period actually used, as resolved by the server (shown in the month pickers).
  const resolved = report && report.view !== 'month' ? report.data : null
  const latestActualMonth = report?.data.latestActualMonth ?? 0
  const shownFrom = fromMonth ?? resolved?.fromMonth ?? 1
  const shownTo = toMonth ?? resolved?.toMonth ?? 12

  const exportUrl =
    view === 'department'
      ? varianceExportUrl.byDepartment(periodQuery)
      : view === 'account'
        ? varianceExportUrl.byAccount(periodQuery)
        : varianceExportUrl.byMonth(monthlyQuery)

  function changeYear(nextYear: number) {
    setYear(nextYear)
    setFromMonth(undefined)
    setToMonth(undefined)
  }

  function changeFrom(month: number) {
    setFromMonth(month)
    setToMonth(Math.max(month, shownTo))
  }

  function changeTo(month: number) {
    setToMonth(month)
    setFromMonth(Math.min(month, shownFrom))
  }

  function showTrend(department: string | undefined, account: string) {
    setMonthlyScope({ department, account })
    setView('month')
  }

  return (
    <section aria-labelledby="variance-heading">
      <h2 id="variance-heading">Budget vs. actual</h2>

      <div className="toolbar">
        <label>
          Fiscal year
          <select
            value={year}
            onChange={(e) => changeYear(Number(e.target.value))}
          >
            {YEARS.map((y) => (
              <option key={y} value={y}>
                FY{y}
              </option>
            ))}
          </select>
        </label>
        {view !== 'month' && (
          <>
            <label>
              From
              <select
                value={shownFrom}
                onChange={(e) => changeFrom(Number(e.target.value))}
              >
                {MONTH_LONG.map((name, index) => (
                  <option key={name} value={index + 1}>
                    {name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              To
              <select
                value={shownTo}
                onChange={(e) => changeTo(Number(e.target.value))}
              >
                {MONTH_LONG.map((name, index) => (
                  <option key={name} value={index + 1}>
                    {name}
                  </option>
                ))}
              </select>
            </label>
          </>
        )}
        <a className="button" href={exportUrl} download>
          Export to Excel
        </a>
      </div>

      <div className="tabs" role="tablist" aria-label="Report view">
        {VIEWS.map((option) => (
          <button
            key={option.id}
            type="button"
            role="tab"
            aria-selected={view === option.id}
            className={view === option.id ? 'tab tab-active' : 'tab'}
            onClick={() => setView(option.id)}
          >
            {option.label}
          </button>
        ))}
      </div>

      {report && (
        <ActualsNote
          year={year}
          latestActualMonth={latestActualMonth}
          toMonth={view === 'month' ? undefined : shownTo}
        />
      )}

      {error && (
        <p role="alert" className="message message-error">
          Could not load the report: {error}
        </p>
      )}

      {loading && <p>Loading…</p>}

      {!loading && report?.view === 'department' && (
        <DepartmentView report={report.data} onShowTrend={showTrend} />
      )}
      {!loading && report?.view === 'account' && (
        <AccountView
          report={report.data}
          onShowTrend={(account) => showTrend(undefined, account)}
        />
      )}
      {!loading && report && report.view !== 'month' && (
        <CommentaryPanel
          key={`${year}-${report.data.fromMonth}-${report.data.toMonth}`}
          query={{
            year,
            fromMonth: report.data.fromMonth,
            toMonth: report.data.toMonth,
          }}
        />
      )}
      {!loading && report?.view === 'month' && (
        <MonthlyView
          report={report.data}
          scope={monthlyScope}
          accounts={accounts}
          departments={departments}
          onScopeChange={setMonthlyScope}
        />
      )}
    </section>
  )
}

/** Explains which months have actuals, and warns when the range runs past them (those count as zero). */
function ActualsNote({
  year,
  latestActualMonth,
  toMonth,
}: {
  year: number
  latestActualMonth: number
  toMonth?: number
}) {
  if (latestActualMonth === 0) {
    return (
      <p className="message message-warning">
        No actuals have been recorded for FY{year} yet, so every actual counts
        as zero.
      </p>
    )
  }
  if (toMonth !== undefined && toMonth > latestActualMonth) {
    return (
      <p className="message message-warning">
        Actuals are recorded through {monthName(latestActualMonth)}. Months
        after that count as zero actuals in this range.
      </p>
    )
  }
  return (
    <p className="hint">
      Actuals recorded through {monthName(latestActualMonth)}.
    </p>
  )
}
