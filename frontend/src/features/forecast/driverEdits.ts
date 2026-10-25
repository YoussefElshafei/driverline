import type {
  AccountType,
  DriverLine,
  DriverLineRequest,
  ForecastMethod,
} from '../../api/types'
import { multiplyMoney, parseDecimal, parseRate } from '../../lib/decimal'

/** The text in a scenario's driver inputs. */
export interface ValueDraft {
  quantity: string
  rate: string
  percent: string
}

/** A line as the user is editing it: its method and the inputs for every scenario. */
export interface LineDraft {
  method: ForecastMethod
  values: Record<string, ValueDraft>
}

export type DraftErrors = Record<string, string>

const QUANTITY_RULES = { maxDecimals: 2 }
const PERCENT_RULES = { maxDecimals: 4, max: 100 }

export function lineKey(line: {
  departmentCode: string
  accountCode: string
}): string {
  return `${line.departmentCode}|${line.accountCode}`
}

/** Builds the editable draft from the server's line, showing numbers without padding zeros ("62", "7.6"). */
export function draftFrom(
  line: DriverLine,
  scenarioCodes: string[],
): LineDraft {
  const values: Record<string, ValueDraft> = {}
  for (const code of scenarioCodes) {
    const stored = line.values[code]
    values[code] = {
      quantity: tidy(stored?.quantity ?? null, QUANTITY_RULES.maxDecimals),
      rate: stored?.rate ?? '',
      percent: tidy(stored?.percent ?? null, PERCENT_RULES.maxDecimals),
    }
  }
  return { method: line.method, values }
}

/** Errors keyed "SCENARIO.field" (e.g. "BEST.rate"). Only the inputs the method uses are checked. */
export function validateDraft(
  draft: LineDraft,
  scenarioCodes: string[],
): DraftErrors {
  const errors: DraftErrors = {}
  for (const code of scenarioCodes) {
    const value = draft.values[code]
    if (draft.method === 'UNITS_X_RATE') {
      addError(
        errors,
        `${code}.quantity`,
        parseDecimal(value.quantity, QUANTITY_RULES),
      )
      addError(errors, `${code}.rate`, parseRate(value.rate))
    } else if (draft.method === 'PERCENT_OF_REVENUE') {
      addError(
        errors,
        `${code}.percent`,
        parseDecimal(value.percent, PERCENT_RULES),
      )
    }
  }
  return errors
}

/** Revenue can't be a percentage of revenue, so that method isn't offered for revenue lines. */
export function allowedMethods(accountType: AccountType): ForecastMethod[] {
  return accountType === 'REVENUE'
    ? ['UNITS_X_RATE', 'BUDGET']
    : ['UNITS_X_RATE', 'PERCENT_OF_REVENUE', 'BUDGET']
}

export function isChanged(
  draft: LineDraft,
  original: LineDraft,
  scenarioCodes: string[],
): boolean {
  if (draft.method !== original.method) {
    return true
  }
  return scenarioCodes.some((code) => {
    const now = draft.values[code]
    const before = original.values[code]
    if (draft.method === 'UNITS_X_RATE') {
      return (
        normalized(now.quantity, parseDecimal(now.quantity, QUANTITY_RULES)) !==
          normalized(
            before.quantity,
            parseDecimal(before.quantity, QUANTITY_RULES),
          ) ||
        normalized(now.rate, parseRate(now.rate)) !==
          normalized(before.rate, parseRate(before.rate))
      )
    }
    if (draft.method === 'PERCENT_OF_REVENUE') {
      return (
        normalized(now.percent, parseDecimal(now.percent, PERCENT_RULES)) !==
        normalized(before.percent, parseDecimal(before.percent, PERCENT_RULES))
      )
    }
    return false
  })
}

/** The PUT /api/forecast/drivers line for a valid draft; only the values the method uses are sent. */
export function toRequest(
  line: DriverLine,
  draft: LineDraft,
  scenarioCodes: string[],
): DriverLineRequest {
  const values: DriverLineRequest['values'] = {}
  if (draft.method !== 'BUDGET') {
    for (const code of scenarioCodes) {
      const value = draft.values[code]
      values[code] =
        draft.method === 'UNITS_X_RATE'
          ? {
              quantity: valueOf(parseDecimal(value.quantity, QUANTITY_RULES)),
              rate: valueOf(parseRate(value.rate)),
              percent: null,
            }
          : {
              quantity: null,
              rate: null,
              percent: valueOf(parseDecimal(value.percent, PERCENT_RULES)),
            }
    }
  }
  return {
    accountCode: line.accountCode,
    departmentCode: line.departmentCode,
    method: draft.method,
    quantityLabel: line.quantityLabel,
    rateLabel: line.rateLabel,
    values,
  }
}

/** quantity x rate for one scenario, or null while either input is invalid. */
export function monthlyPreview(value: ValueDraft): string | null {
  const quantity = parseDecimal(value.quantity, QUANTITY_RULES)
  const rate = parseRate(value.rate)
  return quantity.ok && rate.ok
    ? multiplyMoney(quantity.value, rate.value)
    : null
}

function tidy(stored: string | null, maxDecimals: number): string {
  if (stored === null) {
    return ''
  }
  const result = parseDecimal(stored, { maxDecimals })
  return result.ok ? result.value : stored
}

function addError(
  errors: DraftErrors,
  field: string,
  result: { ok: true } | { ok: false; error: string },
) {
  if (!result.ok) {
    errors[field] = result.error
  }
}

function normalized(
  raw: string,
  result: { ok: true; value: string } | { ok: false },
): string {
  return result.ok ? result.value : `invalid:${raw}`
}

function valueOf(
  result: { ok: true; value: string } | { ok: false },
): string | null {
  return result.ok ? result.value : null
}
