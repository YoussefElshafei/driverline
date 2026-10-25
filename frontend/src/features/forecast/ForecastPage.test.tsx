import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type {
  DriverGrid,
  ForecastReport,
  ForecastSummary,
  OutlookLine,
  ScenarioComparison,
} from '../../api/types'
import { ForecastPage } from './ForecastPage'

function outlook(
  budget: string,
  value: string,
  variance: string,
  variancePercent: string,
  favorability: OutlookLine['favorability'],
): OutlookLine {
  return { budget, outlook: value, variance, variancePercent, favorability }
}

function summary(net: OutlookLine): ForecastSummary {
  return {
    revenue: outlook(
      '4284000.00',
      '4200000.00',
      '-84000.00',
      '-2.0',
      'UNFAVORABLE',
    ),
    expenses: outlook(
      '3890880.00',
      '3950000.00',
      '59120.00',
      '1.5',
      'UNFAVORABLE',
    ),
    netIncome: net,
  }
}

const COMPARISON: ScenarioComparison = {
  year: 2026,
  latestActualMonth: 9,
  scenarios: [
    {
      scenarioCode: 'BASE',
      scenarioName: 'Base',
      summary: summary(
        outlook('393120.00', '250000.00', '-143120.00', '-36.4', 'UNFAVORABLE'),
      ),
    },
    {
      scenarioCode: 'BEST',
      scenarioName: 'Best',
      summary: summary(
        outlook('393120.00', '300000.00', '-93120.00', '-23.7', 'UNFAVORABLE'),
      ),
    },
    {
      scenarioCode: 'WORST',
      scenarioName: 'Worst',
      summary: summary(
        outlook('393120.00', '180000.00', '-213120.00', '-54.2', 'UNFAVORABLE'),
      ),
    },
  ],
}

function report(
  scenarioCode: string,
  scenarioName: string,
  october: string,
): ForecastReport {
  return {
    year: 2026,
    scenarioCode,
    scenarioName,
    latestActualMonth: 9,
    summary: COMPARISON.scenarios[0].summary,
    departments: [
      {
        departmentCode: 'SALES',
        departmentName: 'Sales & Account Management',
        netContribution: outlook(
          '3900000.00',
          '3800000.00',
          '-100000.00',
          '-2.6',
          'UNFAVORABLE',
        ),
        lines: [
          {
            accountCode: '4000',
            accountName: 'Contract Cleaning Revenue',
            accountType: 'REVENUE',
            method: 'UNITS_X_RATE',
            months: Array.from({ length: 12 }, (_, i) => ({
              month: i + 1,
              amount: i < 9 ? '300000.00' : october,
              actual: i < 9,
            })),
            outlook: outlook(
              '3727080.00',
              '3574200.00',
              '-152880.00',
              '-4.1',
              'UNFAVORABLE',
            ),
          },
        ],
      },
    ],
  }
}

const GRID: DriverGrid = {
  year: 2026,
  latestActualMonth: 9,
  scenarios: [
    { code: 'BASE', name: 'Base', description: '' },
    { code: 'BEST', name: 'Best', description: '' },
    { code: 'WORST', name: 'Worst', description: '' },
  ],
  lines: [
    {
      accountCode: '4000',
      accountName: 'Contract Cleaning Revenue',
      accountType: 'REVENUE',
      departmentCode: 'SALES',
      departmentName: 'Sales & Account Management',
      method: 'UNITS_X_RATE',
      quantityLabel: 'Active contracts',
      rateLabel: 'Avg monthly contract value',
      configured: true,
      values: {
        BASE: { quantity: '62.00', rate: '4700.00', percent: null },
        BEST: { quantity: '64.00', rate: '4725.00', percent: null },
        WORST: { quantity: '60.00', rate: '4650.00', percent: null },
      },
      averageOpenMonthBudget: '303300.00',
    },
    {
      accountCode: '6000',
      accountName: 'Rent',
      accountType: 'EXPENSE',
      departmentCode: 'ADMIN',
      departmentName: 'Administration',
      method: 'BUDGET',
      quantityLabel: null,
      rateLabel: null,
      configured: false,
      values: {},
      averageOpenMonthBudget: '12600.00',
    },
  ],
}

function json(body: unknown) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

describe('ForecastPage', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchMock = vi.fn((url: string, init?: RequestInit) => {
      if (url.startsWith('/api/forecast/compare')) return json(COMPARISON)
      if (url.startsWith('/api/forecast/drivers') && init?.method === 'PUT') {
        return json({ year: 2026, linesSaved: 1 })
      }
      if (url.startsWith('/api/forecast/drivers')) return json(GRID)
      if (url.includes('scenario=BEST'))
        return json(report('BEST', 'Best', '302400.00'))
      return json(report('BASE', 'Base', '291400.00'))
    })
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('compares the scenarios and shows the selected one month by month', async () => {
    render(<ForecastPage />)

    const best = await screen.findByRole('button', { name: 'Best scenario' })
    expect(best).toHaveTextContent('$300,000.00')
    expect(
      screen.getByRole('button', { name: 'Base scenario' }),
    ).toHaveAttribute('aria-pressed', 'true')

    const october = await screen.findByTestId('SALES-4000-10')
    expect(october).toHaveTextContent('$291,400')
    expect(october).toHaveClass('cell-forecast')
    expect(screen.getByTestId('SALES-4000-9')).not.toHaveClass('cell-forecast')
    expect(
      screen.getByText(/forecast for October to December/),
    ).toBeInTheDocument()
  })

  it('switches scenario when a card is clicked', async () => {
    const user = userEvent.setup()
    render(<ForecastPage />)

    await user.click(
      await screen.findByRole('button', { name: 'Best scenario' }),
    )

    expect(
      await screen.findByRole('heading', { name: 'Best scenario' }),
    ).toBeInTheDocument()
    expect(screen.getByTestId('SALES-4000-10')).toHaveTextContent('$302,400')
    expect(
      screen.getByRole('link', { name: 'Export to Excel' }),
    ).toHaveAttribute('href', '/api/forecast/export?year=2026&scenario=BEST')
  })

  it('previews a driver change and saves only the changed line', async () => {
    const user = userEvent.setup()
    render(<ForecastPage />)
    const contracts = await screen.findByLabelText(
      'Contract Cleaning Revenue, Sales & Account Management, Base, Active contracts',
    )

    expect(contracts).toHaveValue('62')
    await user.clear(contracts)
    await user.type(contracts, '63')

    expect(screen.getByTestId('preview-SALES|4000-BASE')).toHaveTextContent(
      '= $296,100.00/mo',
    )
    await user.click(screen.getByRole('button', { name: 'Save drivers (1)' }))

    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent(
        'Saved drivers for 1 line.',
      ),
    )
    const save = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')
    const body = JSON.parse(save?.[1]?.body as string)
    expect(body.lines).toHaveLength(1)
    expect(body.lines[0]).toMatchObject({
      accountCode: '4000',
      method: 'UNITS_X_RATE',
      values: { BASE: { quantity: '63', rate: '4700.00', percent: null } },
    })
  })

  it('blocks saving while a driver is invalid', async () => {
    const user = userEvent.setup()
    render(<ForecastPage />)
    const rate = await screen.findByLabelText(
      'Contract Cleaning Revenue, Sales & Account Management, Worst, Avg monthly contract value',
    )

    await user.clear(rate)
    await user.type(rate, '-5')

    expect(rate).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByRole('button', { name: /Save drivers/ })).toBeDisabled()
  })

  it('does not offer percent of revenue for a revenue line, but does for an expense', async () => {
    render(<ForecastPage />)

    const revenueMethod = await screen.findByLabelText(
      'Method for Contract Cleaning Revenue, Sales & Account Management',
    )
    const rentMethod = screen.getByLabelText('Method for Rent, Administration')

    expect(revenueMethod).not.toHaveTextContent('% of revenue')
    expect(rentMethod).toHaveTextContent('% of revenue')
    expect(screen.getAllByText('Budget (avg $12,600.00/mo)')).toHaveLength(3)
  })
})
