import { describe, expect, it } from 'vitest'
import type { BudgetGridRow } from '../../api/types'
import {
  applyEdit,
  cellError,
  changedEntries,
  displayValue,
  hasInvalidEdits,
  rowTotal,
} from './budgetEdits'

function row(months: (string | null)[]): BudgetGridRow {
  const padded = [...months, ...Array(12 - months.length).fill(null)]
  return {
    accountCode: '6100',
    accountName: 'Marketing',
    accountType: 'EXPENSE',
    departmentCode: 'SALES',
    departmentName: 'Sales & Account Management',
    months: padded,
    total: '0.00',
  }
}

describe('budget edits', () => {
  it('shows the saved amount until the user edits a cell', () => {
    const marketing = row(['4800.00'])
    const edits = applyEdit({}, marketing, 0, '5,000')

    expect(displayValue(marketing, 0, {})).toBe('4800.00')
    expect(displayValue(marketing, 0, edits)).toBe('5,000')
    expect(displayValue(marketing, 1, edits)).toBe('')
  })

  it('forgets an edit when the original value is typed back', () => {
    const marketing = row(['4800.00', null])

    expect(applyEdit({ 'SALES|6100|0': '1' }, marketing, 0, '4800')).toEqual({})
    expect(applyEdit({}, marketing, 1, '  ')).toEqual({})
  })

  it('reports invalid input only for edited cells', () => {
    const marketing = row(['4800.00'])
    const edits = applyEdit({}, marketing, 0, '12.345')

    expect(cellError(marketing, 0, edits)).toBe('At most 2 decimal places')
    expect(cellError(marketing, 1, edits)).toBeNull()
    expect(hasInvalidEdits([marketing], edits)).toBe(true)
  })

  it('totals valid edits exactly and keeps the saved value for invalid ones', () => {
    const marketing = row(['0.10', '0.20', '100.00'])
    let edits = applyEdit({}, marketing, 0, '0.15')
    edits = applyEdit(edits, marketing, 2, 'oops')

    // 0.15 + 0.20 + 100.00 (invalid edit ignored)
    expect(rowTotal(marketing, edits)).toBe('100.35')
  })

  it('sends only valid cells whose amount actually changed', () => {
    const marketing = row(['4800.00', '5000.00', null])
    let edits = applyEdit({}, marketing, 0, '$5,100')
    edits = { ...edits, 'SALES|6100|1': '5000' } // same amount, different text
    edits = applyEdit(edits, marketing, 2, '750.5')
    edits = applyEdit(edits, marketing, 3, 'bad')

    expect(changedEntries([marketing], edits)).toEqual([
      {
        accountCode: '6100',
        departmentCode: 'SALES',
        month: 1,
        amount: '5100.00',
      },
      {
        accountCode: '6100',
        departmentCode: 'SALES',
        month: 3,
        amount: '750.50',
      },
    ])
  })
})
