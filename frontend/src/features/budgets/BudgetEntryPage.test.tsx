import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { BudgetGrid } from '../../api/types'
import { BudgetEntryPage } from './BudgetEntryPage'

const ACCOUNTS = [
  { code: '4000', name: 'Contract Cleaning Revenue', type: 'REVENUE' },
  { code: '6100', name: 'Marketing', type: 'EXPENSE' },
]
const DEPARTMENTS = [{ code: 'SALES', name: 'Sales & Account Management' }]

function months(first: string, second: string): (string | null)[] {
  return [first, second, ...Array<null>(10).fill(null)]
}

const GRID_2026: BudgetGrid = {
  year: 2026,
  departmentCode: null,
  rows: [
    {
      accountCode: '6100',
      accountName: 'Marketing',
      accountType: 'EXPENSE',
      departmentCode: 'SALES',
      departmentName: 'Sales & Account Management',
      months: months('5040.00', '8190.00'),
      total: '13230.00',
    },
  ],
}

function json(body: unknown, status = 200) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

describe('BudgetEntryPage', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchMock = vi.fn((url: string, init?: RequestInit) => {
      if (url === '/api/accounts') return json(ACCOUNTS)
      if (url === '/api/departments') return json(DEPARTMENTS)
      if (url.startsWith('/api/budgets?')) {
        const department = new URL(url, 'http://test').searchParams.get(
          'department',
        )
        return json({ ...GRID_2026, departmentCode: department })
      }
      if (url === '/api/budgets' && init?.method === 'PUT') {
        return json({ year: 2026, created: 0, updated: 1 })
      }
      return json({}, 404)
    })
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('loads the budget grid and shows the row total', async () => {
    render(<BudgetEntryPage />)

    expect(
      await screen.findByLabelText(
        'Marketing, Sales & Account Management, Jan',
      ),
    ).toHaveValue('5,040.00')
    expect(screen.getByTestId('total-SALES-6100')).toHaveTextContent(
      '$13,230.00',
    )
  })

  it('updates the total as the user types and saves only the changed cell', async () => {
    const user = userEvent.setup()
    render(<BudgetEntryPage />)
    const january = await screen.findByLabelText(
      'Marketing, Sales & Account Management, Jan',
    )

    await user.clear(january)
    await user.type(january, '6,000.5')

    expect(screen.getByTestId('total-SALES-6100')).toHaveTextContent(
      '$14,190.50',
    )
    await user.click(screen.getByRole('button', { name: 'Save changes (1)' }))

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/budgets',
        expect.objectContaining({ method: 'PUT' }),
      ),
    )
    const saveCall = fetchMock.mock.calls.find(
      ([url, init]) => url === '/api/budgets' && init?.method === 'PUT',
    )
    expect(JSON.parse(saveCall?.[1]?.body as string)).toEqual({
      year: 2026,
      entries: [
        {
          accountCode: '6100',
          departmentCode: 'SALES',
          month: 1,
          amount: '6000.50',
        },
      ],
    })
    expect(await screen.findByRole('status')).toHaveTextContent(
      'Saved 1 change (0 new, 1 updated).',
    )
  })

  it('flags invalid input and blocks saving', async () => {
    const user = userEvent.setup()
    render(<BudgetEntryPage />)
    const february = await screen.findByLabelText(
      'Marketing, Sales & Account Management, Feb',
    )

    await user.clear(february)
    await user.type(february, '12.345')

    expect(february).toHaveAttribute('aria-invalid', 'true')
    expect(
      screen.getByText('Fix the highlighted cells before saving.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Save changes/ })).toBeDisabled()
  })

  it('shows server validation errors against the cell they belong to', async () => {
    fetchMock.mockImplementation((url: string, init?: RequestInit) => {
      if (url === '/api/budgets' && init?.method === 'PUT') {
        return json(
          {
            title: 'Validation failed',
            status: 400,
            detail: 'Some budget entries are invalid. Nothing was saved.',
            errors: [{ field: 'entries[0].amount', message: 'is too large' }],
          },
          400,
        )
      }
      if (url === '/api/accounts') return json(ACCOUNTS)
      if (url === '/api/departments') return json(DEPARTMENTS)
      return json(GRID_2026)
    })
    const user = userEvent.setup()
    render(<BudgetEntryPage />)
    const january = await screen.findByLabelText(
      'Marketing, Sales & Account Management, Jan',
    )

    await user.clear(january)
    await user.type(january, '1')
    await user.click(screen.getByRole('button', { name: /Save changes/ }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Nothing was saved.')
    expect(
      within(alert).getByText(
        'Marketing (Sales & Account Management) – Jan: is too large',
      ),
    ).toBeInTheDocument()
  })

  it('lets the user add an account row once a department is chosen', async () => {
    const user = userEvent.setup()
    render(<BudgetEntryPage />)
    await screen.findByLabelText('Marketing, Sales & Account Management, Jan')

    await user.selectOptions(screen.getByLabelText('Department'), 'SALES')
    await user.selectOptions(
      await screen.findByLabelText('Add account'),
      '4000',
    )
    await user.click(screen.getByRole('button', { name: 'Add row' }))

    expect(
      screen.getByLabelText(
        'Contract Cleaning Revenue, Sales & Account Management, Jan',
      ),
    ).toHaveValue('')
  })
})
