import { describe, expect, it } from 'vitest'
import {
  formatMoney,
  fromCents,
  parseMoney,
  sumAmounts,
  toCents,
} from './money'

describe('parseMoney', () => {
  it.each([
    ['1234.5', '1234.50'],
    ['1234', '1234.00'],
    ['1,234.56', '1234.56'],
    ['$1,234.56', '1234.56'],
    ['  42.10 ', '42.10'],
    ['-12', '-12.00'],
    ['(1,500.25)', '-1500.25'],
    ['007.10', '7.10'],
    ['1.230', '1.23'],
    ['0', '0.00'],
    ['-0', '0.00'],
  ])('accepts %j as %j', (input, expected) => {
    expect(parseMoney(input)).toEqual({ ok: true, amount: expected })
  })

  it.each([
    ['', 'Enter an amount (use 0 for no budget)'],
    ['abc', 'Not a valid amount'],
    ['1.2.3', 'Not a valid amount'],
    ['(-5)', 'Not a valid amount'],
    ['12.345', 'At most 2 decimal places'],
    ['10000000000000', 'Amount is too large'],
  ])('rejects %j', (input, error) => {
    expect(parseMoney(input)).toEqual({ ok: false, error })
  })
})

describe('cents conversion', () => {
  it('round-trips amounts exactly', () => {
    expect(toCents('1234.56')).toBe(123456)
    expect(toCents('-0.05')).toBe(-5)
    expect(fromCents(123456)).toBe('1234.56')
    expect(fromCents(-5)).toBe('-0.05')
  })
})

describe('sumAmounts', () => {
  it('adds exactly where floating point would drift', () => {
    // 0.1 + 0.2 in floating point is 0.30000000000000004
    expect(sumAmounts(['0.10', '0.20'])).toBe('0.30')
  })

  it('skips months with no amount', () => {
    expect(sumAmounts(['100.00', null, '-25.50', null])).toBe('74.50')
  })

  it('returns zero for no amounts', () => {
    expect(sumAmounts([])).toBe('0.00')
  })
})

describe('formatMoney', () => {
  it.each([
    ['1234567.5', '$1,234,567.50'],
    ['0', '$0.00'],
    ['-12.00', '-$12.00'],
    ['999.99', '$999.99'],
  ])('formats %j as %j', (input, expected) => {
    expect(formatMoney(input)).toBe(expected)
  })
})
