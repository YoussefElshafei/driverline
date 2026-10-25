import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ActualsImportPage } from './ActualsImportPage'

function respondWith(body: unknown, status: number) {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json' },
      }),
    ),
  )
}

const csvFile = () =>
  new File(
    ['year,month,account_code,department_code,amount\n'],
    'october.csv',
    {
      type: 'text/csv',
    },
  )

describe('ActualsImportPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('disables Import until a file is chosen', () => {
    render(<ActualsImportPage />)

    expect(screen.getByRole('button', { name: 'Import' })).toBeDisabled()
  })

  it('uploads the file and shows a summary', async () => {
    respondWith(
      {
        fileName: 'october.csv',
        rowsImported: 15,
        created: 15,
        updated: 0,
        periods: ['2026-10'],
      },
      200,
    )
    const user = userEvent.setup()
    render(<ActualsImportPage />)

    await user.upload(screen.getByLabelText('Actuals file'), csvFile())
    await user.click(screen.getByRole('button', { name: 'Import' }))

    const status = await screen.findByRole('status')
    expect(status).toHaveTextContent(
      'Imported 15 rows from october.csv: 15 new, 0 updated.',
    )
    expect(status).toHaveTextContent('Periods: 2026-10')
    const [, init] = vi.mocked(fetch).mock.calls[0]
    expect((init?.body as FormData).get('file')).toBeInstanceOf(File)
  })

  it('lists every row error when the file is rejected', async () => {
    respondWith(
      {
        title: 'Import failed',
        status: 422,
        detail: 'Found 2 problems in the file. No rows were imported.',
        errors: [
          {
            row: 3,
            column: 'month',
            value: '13',
            message: 'must be between 1 and 12',
          },
          {
            row: 4,
            column: 'amount',
            value: '12.345',
            message: "'12.345' has more than 2 decimal places",
          },
        ],
      },
      422,
    )
    const user = userEvent.setup()
    render(<ActualsImportPage />)

    await user.upload(screen.getByLabelText('Actuals file'), csvFile())
    await user.click(screen.getByRole('button', { name: 'Import' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('No rows were imported.')
    const rows = within(alert).getAllByRole('row')
    expect(rows).toHaveLength(3) // header + 2 errors
    expect(rows[1]).toHaveTextContent('3month13must be between 1 and 12')
  })

  it('links to the CSV template', () => {
    render(<ActualsImportPage />)

    expect(
      screen.getByRole('link', { name: 'Download a CSV template' }),
    ).toHaveAttribute('href', '/api/actuals/import/template')
  })
})
