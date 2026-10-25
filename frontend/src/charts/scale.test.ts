import { describe, expect, it } from 'vitest'
import { formatAxisMoney, linearScale, niceTicks } from './scale'

describe('linearScale', () => {
  it('maps the domain onto the range, including an inverted range for SVG y', () => {
    const y = linearScale([0, 100], [200, 0])

    expect(y(0)).toBe(200)
    expect(y(50)).toBe(100)
    expect(y(100)).toBe(0)
  })

  it('does not divide by zero for an empty domain', () => {
    expect(linearScale([5, 5], [0, 10])(5)).toBe(0)
  })
})

describe('niceTicks', () => {
  it('uses round steps and always includes zero', () => {
    expect(niceTicks(12_000, 87_000)).toEqual([
      0, 20_000, 40_000, 60_000, 80_000, 100_000,
    ])
  })

  it('spans negative and positive values', () => {
    const ticks = niceTicks(-30_000, 45_000)

    expect(ticks[0]).toBeLessThanOrEqual(-30_000)
    expect(ticks.at(-1)).toBeGreaterThanOrEqual(45_000)
    expect(ticks).toContain(0)
  })

  it('returns just zero when every value is zero', () => {
    expect(niceTicks(0, 0)).toEqual([0])
  })
})

describe('formatAxisMoney', () => {
  it.each([
    [0, '$0'],
    [950, '$950'],
    [300_000, '$300K'],
    [12_500, '$12.5K'],
    [1_250_000, '$1.25M'],
    [-40_000, '-$40K'],
  ])('%d -> %s', (value, expected) => {
    expect(formatAxisMoney(value)).toBe(expected)
  })
})
