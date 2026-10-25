/**
 * Money helpers. The API sends amounts as strings ("1234.50"), never JSON numbers, because JavaScript numbers are
 * binary floating point (0.1 + 0.2 === 0.30000000000000004). We do all arithmetic in whole cents, which are integers
 * and therefore exact, and only turn them back into strings for display or sending.
 */

export type MoneyParseResult =
  { ok: true; amount: string } | { ok: false; error: string }

const PLAIN_NUMBER = /^-?\d+(\.\d+)?$/
const MAX_WHOLE_DIGITS = 13

/**
 * Parses what a user typed into a canonical 2-decimal amount string.
 * Accepts "1234.5", "1,234.50", "$1,234.50", "-12" and accounting-style "(12.00)". Mirrors the backend's MoneyParser.
 */
export function parseMoney(input: string): MoneyParseResult {
  let text = input.trim()
  if (text === '') {
    return { ok: false, error: 'Enter an amount (use 0 for no budget)' }
  }
  const accountingNegative = text.startsWith('(') && text.endsWith(')')
  if (accountingNegative) {
    text = text.slice(1, -1)
  }
  text = text.replaceAll('$', '').replaceAll(',', '').trim()

  if (
    !PLAIN_NUMBER.test(text) ||
    (accountingNegative && text.startsWith('-'))
  ) {
    return { ok: false, error: 'Not a valid amount' }
  }
  const negative = accountingNegative || text.startsWith('-')
  const [whole, fraction = ''] = text.replace('-', '').split('.')
  const trimmedFraction = fraction.replace(/0+$/, '')
  if (trimmedFraction.length > 2) {
    return { ok: false, error: 'At most 2 decimal places' }
  }
  const normalizedWhole = whole.replace(/^0+(?=\d)/, '')
  if (normalizedWhole.length > MAX_WHOLE_DIGITS) {
    return { ok: false, error: 'Amount is too large' }
  }
  const isZero = /^0*$/.test(normalizedWhole + trimmedFraction)
  const sign = negative && !isZero ? '-' : ''
  return {
    ok: true,
    amount: `${sign}${normalizedWhole}.${trimmedFraction.padEnd(2, '0')}`,
  }
}

/** "1234.56" -> 123456. Expects a canonical amount string (as sent by the API or returned by parseMoney). */
export function toCents(amount: string): number {
  const negative = amount.startsWith('-')
  const [whole, fraction = ''] = amount.replace('-', '').split('.')
  const cents =
    Number(whole) * 100 + Number(fraction.padEnd(2, '0').slice(0, 2))
  return negative ? -cents : cents
}

/** 123456 -> "1234.56" */
export function fromCents(cents: number): string {
  const sign = cents < 0 ? '-' : ''
  const absolute = Math.abs(cents)
  const whole = Math.floor(absolute / 100)
  const fraction = String(absolute % 100).padStart(2, '0')
  return `${sign}${whole}.${fraction}`
}

/** Exact sum of amount strings; null entries (months with no budget) are skipped. */
export function sumAmounts(amounts: ReadonlyArray<string | null>): string {
  const totalCents = amounts.reduce(
    (sum, amount) => (amount === null ? sum : sum + toCents(amount)),
    0,
  )
  return fromCents(totalCents)
}

/**
 * "156081.46" -> "$156,081" (rounded half-up on the magnitude, in integer cents). For dense tables where cents
 * add noise; totals beside them still show exact cents.
 */
export function formatWholeDollars(amount: string): string {
  const cents = toCents(amount)
  const dollars = Math.floor((Math.abs(cents) + 50) / 100)
  const sign = cents < 0 && dollars > 0 ? '-' : ''
  return `${sign}$${String(dollars).replace(/\B(?=(\d{3})+(?!\d))/g, ',')}`
}

/** "154812" -> "154,812.00": an editable amount with separators but no "$" (parseMoney reads it back). */
export function formatAmount(amount: string): string {
  return formatMoney(amount).replace('$', '')
}

/** "1234567.5" -> "$1,234,567.50"; "-12.00" -> "-$12.00". String-based, so no rounding can creep in. */
export function formatMoney(amount: string): string {
  const canonical = fromCents(toCents(amount))
  const negative = canonical.startsWith('-')
  const [whole, fraction] = canonical.replace('-', '').split('.')
  const withSeparators = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return `${negative ? '-' : ''}$${withSeparators}.${fraction}`
}
