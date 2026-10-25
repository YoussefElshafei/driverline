import { describe, expect, it } from 'vitest'
import type { ForecastLineRow, ForecastReport } from '../../api/types'
import { cumulative, monthlyNetCents } from './outlookSeries'

function line(
  accountType: ForecastLineRow['accountType'],
  amounts: string[],
): ForecastLineRow {
  return {
    accountCode: '0000',
    accountName: 'Line',
    accountType,
    method: 'BUDGET',
    months: amounts.map((amount, i) => ({
      month: i + 1,
      amount,
      actual: false,
    })),
    outlook: {
      budget: '0.00',
      outlook: '0.00',
      variance: '0.00',
      variancePercent: null,
      favorability: 'ON_BUDGET',
    },
  }
}

function report(lines: ForecastLineRow[][]): ForecastReport {
  return {
    year: 2026,
    scenarioCode: 'BASE',
    scenarioName: 'Base',
    latestActualMonth: 0,
    summary: {} as ForecastReport['summary'],
    departments: lines.map((departmentLines, i) => ({
      departmentCode: `D${i}`,
      departmentName: `Department ${i}`,
      netContribution: departmentLines[0].outlook,
      lines: departmentLines,
    })),
  }
}

const twelve = (amount: string) => Array<string>(12).fill(amount)

describe('monthlyNetCents', () => {
  it('adds revenue and subtracts expenses across departments, exactly in cents', () => {
    const net = monthlyNetCents(
      report([
        [line('REVENUE', twelve('0.10')), line('EXPENSE', twelve('0.30'))],
        [line('REVENUE', twelve('0.20'))],
      ]),
    )

    // 0.10 + 0.20 - 0.30 is exactly zero in cents (it is not in floating point).
    expect(net).toEqual(Array(12).fill(0))
  })

  it('can be negative when expenses exceed revenue', () => {
    const net = monthlyNetCents(
      report([
        [line('REVENUE', twelve('100.00')), line('EXPENSE', twelve('150.25'))],
      ]),
    )

    expect(net[0]).toBe(-5025)
  })
})

describe('cumulative', () => {
  it('returns running totals', () => {
    expect(cumulative([5, -2, 10])).toEqual([5, 3, 13])
  })

  it('returns an empty list for no values', () => {
    expect(cumulative([])).toEqual([])
  })
})
