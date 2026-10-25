import type { ForecastReport, MonthlyVarianceReport } from '../../api/types'
import { toCents } from '../../lib/money'

/** Net income per month (revenue minus expenses), in exact integer cents, January first. */
export function monthlyNetCents(report: ForecastReport): number[] {
  const net = Array<number>(12).fill(0)
  for (const department of report.departments) {
    for (const line of department.lines) {
      const sign = line.accountType === 'REVENUE' ? 1 : -1
      for (const month of line.months) {
        net[month.month - 1] += sign * toCents(month.amount)
      }
    }
  }
  return net
}

/** Running total: [a, b, c] -> [a, a+b, a+b+c]. */
export function cumulative(values: number[]): number[] {
  let total = 0
  return values.map((value) => (total += value))
}

export interface OutlookSeries {
  latestActualMonth: number
  /** Cumulative net income budget, 12 values in cents */
  budget: number[]
  /** Cumulative net income for the selected scenario: actuals, then forecast */
  selected: number[]
  /** Cumulative Best and Worst outlooks, for the range band over forecast months */
  best: number[]
  worst: number[]
}

/** Combines the scenario reports and the monthly budget into the cumulative lines the outlook chart draws. */
export function buildOutlookSeries(
  reports: Record<string, ForecastReport>,
  selected: string,
  budgetTrend: MonthlyVarianceReport,
): OutlookSeries | null {
  const chosen = reports[selected]
  if (!chosen || !reports.BEST || !reports.WORST) {
    return null
  }
  return {
    latestActualMonth: chosen.latestActualMonth,
    budget: cumulative(budgetTrend.months.map((m) => toCents(m.budget))),
    selected: cumulative(monthlyNetCents(chosen)),
    best: cumulative(monthlyNetCents(reports.BEST)),
    worst: cumulative(monthlyNetCents(reports.WORST)),
  }
}
