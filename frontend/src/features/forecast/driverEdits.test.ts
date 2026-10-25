import { describe, expect, it } from 'vitest'
import type { DriverLine } from '../../api/types'
import {
  allowedMethods,
  draftFrom,
  isChanged,
  monthlyPreview,
  toRequest,
  validateDraft,
} from './driverEdits'

const SCENARIOS = ['BASE', 'BEST', 'WORST']

const CONTRACTS: DriverLine = {
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
}

describe('driver edits', () => {
  it('shows stored numbers without padding zeros', () => {
    const draft = draftFrom(CONTRACTS, SCENARIOS)

    expect(draft.values.BASE).toEqual({
      quantity: '62',
      rate: '4700.00',
      percent: '',
    })
  })

  it('previews quantity x rate exactly', () => {
    expect(monthlyPreview({ quantity: '62', rate: '4,700', percent: '' })).toBe(
      '291400.00',
    )
    expect(
      monthlyPreview({ quantity: 'x', rate: '4700', percent: '' }),
    ).toBeNull()
  })

  it('validates only the inputs the method uses', () => {
    const draft = draftFrom(CONTRACTS, SCENARIOS)
    draft.values.BEST.rate = '-1'
    draft.values.WORST.quantity = '1.234'

    expect(validateDraft(draft, SCENARIOS)).toEqual({
      'BEST.rate': 'Enter a number of 0 or more',
      'WORST.quantity': 'At most 2 decimal places',
    })
    expect(validateDraft({ ...draft, method: 'BUDGET' }, SCENARIOS)).toEqual({})
  })

  it('requires a percent for every scenario on percent-of-revenue lines', () => {
    const draft = {
      ...draftFrom(CONTRACTS, SCENARIOS),
      method: 'PERCENT_OF_REVENUE' as const,
    }

    expect(Object.keys(validateDraft(draft, SCENARIOS))).toEqual([
      'BASE.percent',
      'BEST.percent',
      'WORST.percent',
    ])
  })

  it('treats the same value typed differently as unchanged', () => {
    const original = draftFrom(CONTRACTS, SCENARIOS)
    const retyped = draftFrom(CONTRACTS, SCENARIOS)
    retyped.values.BASE.quantity = '62.0'
    retyped.values.BASE.rate = '$4,700'

    expect(isChanged(retyped, original, SCENARIOS)).toBe(false)
    retyped.values.BASE.quantity = '63'
    expect(isChanged(retyped, original, SCENARIOS)).toBe(true)
  })

  it('builds the save request with only the values the method needs', () => {
    const draft = draftFrom(CONTRACTS, SCENARIOS)
    draft.values.BASE.quantity = '63'

    expect(toRequest(CONTRACTS, draft, SCENARIOS).values.BASE).toEqual({
      quantity: '63',
      rate: '4700.00',
      percent: null,
    })
    expect(
      toRequest(CONTRACTS, { ...draft, method: 'BUDGET' }, SCENARIOS).values,
    ).toEqual({})
  })

  it('does not offer percent of revenue for revenue lines', () => {
    expect(allowedMethods('REVENUE')).not.toContain('PERCENT_OF_REVENUE')
    expect(allowedMethods('EXPENSE')).toContain('PERCENT_OF_REVENUE')
  })
})
