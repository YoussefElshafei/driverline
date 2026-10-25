import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type {
  AccountVarianceReport,
  DepartmentVarianceReport,
  MonthlyVarianceReport,
  VarianceLine,
} from '../../api/types'
import { VariancePage } from './VariancePage'

function line(
  budget: string,
  actual: string,
  variance: string,
  variancePercent: string | null,
  favorability: VarianceLine['favorability'],
): VarianceLine {
  return { budget, actual, variance, variancePercent, favorability }
}

const SUMMARY = {
  revenue: line('3000000.00', '2950000.00', '-50000.00', '-1.7', 'UNFAVORABLE'),
  expenses: line('2700000.00', '2680000.00', '-20000.00', '-0.7', 'FAVORABLE'),
  netIncome: line(
    '300000.00',
    '270000.00',
    '-30000.00',
    '-10.0',
    'UNFAVORABLE',
  ),
}

const BY_DEPARTMENT: DepartmentVarianceReport = {
  year: 2026,
  fromMonth: 1,
  toMonth: 9,
  latestActualMonth: 9,
  summary: SUMMARY,
  departments: [
    {
      departmentCode: 'FLEET',
      departmentName: 'Fleet & Equipment',
      netContribution: line(
        '-250000.00',
        '-268000.00',
        '-18000.00',
        '-7.2',
        'UNFAVORABLE',
      ),
      accounts: [
        {
          accountCode: '5300',
          accountName: 'Vehicle & Fuel',
          accountType: 'EXPENSE',
          line: line(
            '140000.00',
            '158000.00',
            '18000.00',
            '12.9',
            'UNFAVORABLE',
          ),
        },
      ],
    },
  ],
}

const BY_ACCOUNT: AccountVarianceReport = {
  year: 2026,
  fromMonth: 1,
  toMonth: 9,
  latestActualMonth: 9,
  summary: SUMMARY,
  revenue: [
    {
      accountCode: '4000',
      accountName: 'Contract Cleaning Revenue',
      accountType: 'REVENUE',
      line: line(
        '2700000.00',
        '2650000.00',
        '-50000.00',
        '-1.9',
        'UNFAVORABLE',
      ),
    },
  ],
  expenses: [],
}

const BY_MONTH: MonthlyVarianceReport = {
  year: 2026,
  departmentCode: 'FLEET',
  accountCode: '5300',
  scope: '5300 Vehicle & Fuel – Fleet & Equipment',
  direction: 'LOWER_IS_BETTER',
  latestActualMonth: 9,
  months: Array.from({ length: 12 }, (_, i) =>
    i < 9
      ? {
          month: i + 1,
          budget: '15000.00',
          closed: true,
          variance: line(
            '15000.00',
            '17000.00',
            '2000.00',
            '13.3',
            'UNFAVORABLE',
          ),
        }
      : { month: i + 1, budget: '15000.00', closed: false, variance: null },
  ),
  closedMonthsTotal: line(
    '135000.00',
    '153000.00',
    '18000.00',
    '13.3',
    'UNFAVORABLE',
  ),
}

function json(body: unknown) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

describe('VariancePage', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchMock = vi.fn((url: string) => {
      if (url === '/api/accounts') return json([])
      if (url === '/api/departments') return json([])
      if (url.startsWith('/api/variance/by-department'))
        return json(BY_DEPARTMENT)
      if (url.startsWith('/api/variance/by-account')) return json(BY_ACCOUNT)
      if (url.startsWith('/api/variance/by-month')) return json(BY_MONTH)
      return json({})
    })
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  function requestedUrls(): string[] {
    return fetchMock.mock.calls.map(([url]) => url as string)
  }

  it('shows the company summary and department totals with flags', async () => {
    render(<VariancePage />)

    const netIncome = await screen.findByRole('article', { name: 'Net income' })
    expect(netIncome).toHaveTextContent('$270,000.00')
    expect(netIncome).toHaveTextContent('-$30,000.00 (-10.0%)')
    expect(netIncome).toHaveTextContent('Unfavorable')

    const fleetRow = screen.getByRole('row', { name: /Fleet & Equipment/ })
    expect(fleetRow).toHaveTextContent('-$18,000.00')
    expect(fleetRow).toHaveTextContent('-7.2%')
    expect(
      screen.getByText('Actuals recorded through September.'),
    ).toBeInTheDocument()
  })

  it('asks the server for the default period and shows the months it chose', async () => {
    render(<VariancePage />)
    await screen.findByRole('article', { name: 'Net income' })

    expect(requestedUrls()).toContain('/api/variance/by-department?year=2026')
    expect(screen.getByLabelText('From')).toHaveValue('1')
    expect(screen.getByLabelText('To')).toHaveValue('9')
  })

  it('drills down from a department to its accounts', async () => {
    const user = userEvent.setup()
    render(<VariancePage />)
    const toggle = await screen.findByRole('button', {
      name: 'Show accounts for Fleet & Equipment',
    })

    expect(screen.queryByText('5300 Vehicle & Fuel')).not.toBeInTheDocument()
    await user.click(toggle)

    expect(toggle).toHaveAttribute('aria-expanded', 'true')
    const fuelRow = screen.getByRole('row', { name: /5300 Vehicle & Fuel/ })
    expect(fuelRow).toHaveTextContent('+$18,000.00')
    expect(fuelRow).toHaveTextContent('Unfavorable')
  })

  it('links the Excel export to the current view and period', async () => {
    const user = userEvent.setup()
    render(<VariancePage />)
    await screen.findByRole('article', { name: 'Net income' })

    await user.selectOptions(screen.getByLabelText('To'), '6')

    expect(
      screen.getByRole('link', { name: 'Export to Excel' }),
    ).toHaveAttribute(
      'href',
      '/api/variance/by-department/export?year=2026&fromMonth=1&toMonth=6',
    )
    await waitFor(() =>
      expect(requestedUrls()).toContain(
        '/api/variance/by-department?year=2026&fromMonth=1&toMonth=6',
      ),
    )
  })

  it('warns when the range includes months without actuals', async () => {
    const user = userEvent.setup()
    render(<VariancePage />)
    await screen.findByRole('article', { name: 'Net income' })

    await user.selectOptions(screen.getByLabelText('To'), '12')

    expect(
      await screen.findByText(/Months after that count as zero actuals/),
    ).toBeInTheDocument()
  })

  it('switches to the account view', async () => {
    const user = userEvent.setup()
    render(<VariancePage />)
    await screen.findByRole('article', { name: 'Net income' })

    await user.click(screen.getByRole('tab', { name: 'By account' }))

    expect(
      await screen.findByRole('row', {
        name: /4000 Contract Cleaning Revenue/,
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('row', { name: /Total revenue/ }),
    ).toBeInTheDocument()
  })

  it('opens the monthly trend for an account from the drill-down', async () => {
    const user = userEvent.setup()
    render(<VariancePage />)
    await user.click(
      await screen.findByRole('button', {
        name: 'Show accounts for Fleet & Equipment',
      }),
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Monthly trend for 5300 Vehicle & Fuel in Fleet & Equipment',
      }),
    )

    expect(
      await screen.findByRole('heading', {
        name: '5300 Vehicle & Fuel – Fleet & Equipment',
      }),
    ).toBeInTheDocument()
    expect(requestedUrls()).toContain(
      '/api/variance/by-month?year=2026&department=FLEET&account=5300',
    )
    const october = screen.getByRole('row', { name: /October/ })
    expect(within(october).getByText('Not closed')).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'By month' })).toHaveAttribute(
      'aria-selected',
      'true',
    )
  })
})
