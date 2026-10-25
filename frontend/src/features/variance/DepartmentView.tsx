import { ChevronRight, ChevronsDownUp, ChevronsUpDown } from 'lucide-react'
import { Fragment, useState } from 'react'
import type { DepartmentVarianceReport } from '../../api/types'
import { VarianceCells, VarianceHeaderCells } from './VarianceCells'

interface DepartmentViewProps {
  report: DepartmentVarianceReport
  onShowTrend: (departmentCode: string, accountCode: string) => void
}

/** Department totals that expand to show each department's accounts (the drill-down). */
export function DepartmentView({ report, onShowTrend }: DepartmentViewProps) {
  const [expanded, setExpanded] = useState<ReadonlySet<string>>(new Set())
  const allExpanded =
    report.departments.length > 0 && expanded.size === report.departments.length

  function toggle(code: string) {
    setExpanded((current) => {
      const next = new Set(current)
      if (next.has(code)) {
        next.delete(code)
      } else {
        next.add(code)
      }
      return next
    })
  }

  function toggleAll() {
    setExpanded(
      allExpanded
        ? new Set()
        : new Set(report.departments.map((d) => d.departmentCode)),
    )
  }

  return (
    <section className="card table-card" aria-labelledby="departments-heading">
      <div className="table-heading">
        <div>
          <h3 id="departments-heading" className="card-title">
            Departments
          </h3>
          <p className="card-subtitle">
            Rows show <strong>net contribution</strong> (revenue minus
            expenses), so spending less than budget is favorable. Expand a
            department to see its accounts.
          </p>
        </div>
        <button
          type="button"
          onClick={toggleAll}
          disabled={report.departments.length === 0}
        >
          {allExpanded ? (
            <ChevronsDownUp size={15} aria-hidden="true" />
          ) : (
            <ChevronsUpDown size={15} aria-hidden="true" />
          )}
          {allExpanded ? 'Collapse all' : 'Expand all'}
        </button>
      </div>
      <div className="table-scroll">
        <table className="report">
          <thead>
            <tr>
              <th scope="col">Department / account</th>
              <VarianceHeaderCells />
              <th scope="col">
                <span className="visually-hidden">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {report.departments.map((department) => {
              const isOpen = expanded.has(department.departmentCode)
              return (
                <Fragment key={department.departmentCode}>
                  <tr className="row-department">
                    <th scope="row">
                      <button
                        type="button"
                        className="toggle"
                        aria-expanded={isOpen}
                        aria-label={`${isOpen ? 'Hide' : 'Show'} accounts for ${department.departmentName}`}
                        onClick={() => toggle(department.departmentCode)}
                      >
                        <ChevronRight size={16} aria-hidden="true" />
                      </button>
                      {department.departmentName}
                    </th>
                    <VarianceCells line={department.netContribution} />
                    <td />
                  </tr>
                  {isOpen &&
                    department.accounts.map((account) => (
                      <tr key={account.accountCode} className="row-account">
                        <th scope="row">
                          {account.accountCode} {account.accountName}
                        </th>
                        <VarianceCells line={account.line} />
                        <td>
                          <button
                            type="button"
                            className="link-button"
                            aria-label={`Monthly trend for ${account.accountCode} ${account.accountName} in ${department.departmentName}`}
                            onClick={() =>
                              onShowTrend(
                                department.departmentCode,
                                account.accountCode,
                              )
                            }
                          >
                            Trend
                          </button>
                        </td>
                      </tr>
                    ))}
                </Fragment>
              )
            })}
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
    </section>
  )
}
