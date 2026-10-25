import { fromCents, parseMoney, toCents } from './money'

export type DecimalParseResult =
  { ok: true; value: string } | { ok: false; error: string }

interface DecimalRules {
  maxDecimals: number
  max?: number
}

const NON_NEGATIVE_NUMBER = /^\d+(\.\d+)?$/

/**
 * Parses a non-negative driver input such as a headcount ("41"), a quantity ("12.5") or a percent ("7.6"),
 * returning it without trailing zeros. Mirrors the backend's validation, so errors show before saving.
 */
export function parseDecimal(
  input: string,
  rules: DecimalRules,
): DecimalParseResult {
  const text = input.trim().replaceAll(',', '')
  if (text === '') {
    return { ok: false, error: 'Required' }
  }
  if (!NON_NEGATIVE_NUMBER.test(text)) {
    return { ok: false, error: 'Enter a number of 0 or more' }
  }
  const [whole, fraction = ''] = text.split('.')
  const trimmedFraction = fraction.replace(/0+$/, '')
  if (trimmedFraction.length > rules.maxDecimals) {
    return { ok: false, error: `At most ${rules.maxDecimals} decimal places` }
  }
  const normalizedWhole = whole.replace(/^0+(?=\d)/, '')
  if (rules.max !== undefined && Number(text) > rules.max) {
    return { ok: false, error: `At most ${rules.max}` }
  }
  return {
    ok: true,
    value: trimmedFraction
      ? `${normalizedWhole}.${trimmedFraction}`
      : normalizedWhole,
  }
}

/** A non-negative money rate such as "4,700" -> "4700.00". */
export function parseRate(input: string): DecimalParseResult {
  if (input.trim().startsWith('-') || input.trim().startsWith('(')) {
    return { ok: false, error: 'Enter a number of 0 or more' }
  }
  const result = parseMoney(input)
  return result.ok
    ? { ok: true, value: result.amount }
    : {
        ok: false,
        error:
          result.error === 'Enter an amount (use 0 for no budget)'
            ? 'Required'
            : result.error,
      }
}

/**
 * quantity x rate, rounded half-up to cents, exactly. Uses BigInt: the quantity in hundredths times the rate in
 * cents gives ten-thousandths of a dollar, which is then rounded to whole cents. Mirrors the backend calculation.
 */
export function multiplyMoney(quantity: string, rate: string): string {
  const [whole, fraction = ''] = quantity.split('.')
  const quantityHundredths =
    BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0').slice(0, 2))
  const product = quantityHundredths * BigInt(toCents(rate))
  return fromCents(Number((product + 50n) / 100n))
}
