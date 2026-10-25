import { describe, expect, it } from 'vitest'
import { multiplyMoney, parseDecimal, parseRate } from './decimal'

describe('parseDecimal', () => {
  it.each([
    ['41', '41'],
    ['62.00', '62'],
    ['12.50', '12.5'],
    ['7.6000', '7.6'],
    ['007', '7'],
  ])('accepts %j as %j', (input, expected) => {
    expect(parseDecimal(input, { maxDecimals: 4 })).toEqual({
      ok: true,
      value: expected,
    })
  })

  it('rejects blanks, negatives, too many decimals and values over the maximum', () => {
    expect(parseDecimal('', { maxDecimals: 2 })).toEqual({
      ok: false,
      error: 'Required',
    })
    expect(parseDecimal('-1', { maxDecimals: 2 })).toEqual({
      ok: false,
      error: 'Enter a number of 0 or more',
    })
    expect(parseDecimal('1.234', { maxDecimals: 2 })).toEqual({
      ok: false,
      error: 'At most 2 decimal places',
    })
    expect(parseDecimal('150', { maxDecimals: 4, max: 100 })).toEqual({
      ok: false,
      error: 'At most 100',
    })
  })
})

describe('parseRate', () => {
  it('accepts money and rejects negatives', () => {
    expect(parseRate('$4,700')).toEqual({ ok: true, value: '4700.00' })
    expect(parseRate('-5')).toEqual({
      ok: false,
      error: 'Enter a number of 0 or more',
    })
    expect(parseRate('')).toEqual({ ok: false, error: 'Required' })
  })
})

describe('multiplyMoney', () => {
  it.each([
    ['62', '4700.00', '291400.00'],
    ['14', '1250.00', '17500.00'],
    ['12.5', '2899.99', '36249.88'],
    ['0', '9999.99', '0.00'],
  ])('%s x %s = %s', (quantity, rate, expected) => {
    expect(multiplyMoney(quantity, rate)).toBe(expected)
  })
})
