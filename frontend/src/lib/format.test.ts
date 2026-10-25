import { describe, expect, it } from 'vitest'
import {
  formatPercent,
  formatSignedMoney,
  formatSignedWholeDollars,
} from './format'

describe('formatPercent', () => {
  it.each([
    ['4.4', '+4.4%'],
    ['-2.5', '-2.5%'],
    ['0.0', '0.0%'],
    [null, 'n/a'],
  ])('formats %j as %j', (input, expected) => {
    expect(formatPercent(input)).toBe(expected)
  })
})

describe('formatSignedMoney', () => {
  it.each([
    ['80.00', '+$80.00'],
    ['-16361.45', '-$16,361.45'],
    ['0.00', '$0.00'],
  ])('formats %j as %j', (input, expected) => {
    expect(formatSignedMoney(input)).toBe(expected)
  })
})

describe('formatSignedWholeDollars', () => {
  it.each([
    ['152880.40', '+$152,880'],
    ['-16361.50', '-$16,362'],
    ['-0.40', '$0'],
    ['0.00', '$0'],
  ])('formats %j as %j', (input, expected) => {
    expect(formatSignedWholeDollars(input)).toBe(expected)
  })
})
