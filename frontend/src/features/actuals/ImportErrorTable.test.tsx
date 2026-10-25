import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ImportErrorTable } from './ImportErrorTable'

describe('ImportErrorTable', () => {
  it('labels whole-file problems as "File" instead of a row number', () => {
    render(
      <ImportErrorTable
        errors={[
          {
            row: null,
            column: null,
            value: null,
            message: 'The file has no data rows.',
          },
          {
            row: 1,
            column: 'amount',
            value: null,
            message: "Missing required column 'amount'",
          },
        ]}
      />,
    )

    const rows = screen.getAllByRole('row')
    expect(rows[1]).toHaveTextContent('File—')
    expect(rows[1]).toHaveTextContent('The file has no data rows.')
    expect(rows[2]).toHaveTextContent(
      "1amount—Missing required column 'amount'",
    )
  })

  it('shows the offending value so the user can find it in the file', () => {
    render(
      <ImportErrorTable
        errors={[
          {
            row: 7,
            column: 'account_code',
            value: '9999',
            message: "Unknown account code '9999'",
          },
        ]}
      />,
    )

    expect(screen.getByText('9999').tagName).toBe('CODE')
  })
})
