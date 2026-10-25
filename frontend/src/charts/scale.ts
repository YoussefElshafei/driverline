/**
 * Small, dependency-free helpers for the SVG charts. Chart geometry only needs floating point (pixels), so amounts
 * are converted with Number() here; every figure a user reads in a tooltip is still formatted from the exact string.
 */

export interface LinearScale {
  (value: number): number
  domain: [number, number]
}

/** Maps [d0, d1] onto [r0, r1]. */
export function linearScale(
  [d0, d1]: [number, number],
  [r0, r1]: [number, number],
): LinearScale {
  const span = d1 - d0 || 1
  const scale = ((value: number) =>
    r0 + ((value - d0) / span) * (r1 - r0)) as LinearScale
  scale.domain = [d0, d1]
  return scale
}

/**
 * Round tick values ("nice" steps of 1, 2, 2.5 or 5 × 10ⁿ) covering min..max, always including zero so bars and
 * the baseline stay honest.
 */
export function niceTicks(min: number, max: number, count = 5): number[] {
  const low = Math.min(0, min)
  const high = Math.max(0, max)
  if (low === high) {
    return [0]
  }
  const rough = (high - low) / count
  const magnitude = 10 ** Math.floor(Math.log10(rough))
  const step =
    [1, 2, 2.5, 5, 10].map((m) => m * magnitude).find((s) => s >= rough) ??
    10 * magnitude
  const start = Math.floor(low / step) * step
  const end = Math.ceil(high / step) * step
  const ticks: number[] = []
  const steps = Math.round((end - start) / step)
  for (let i = 0; i <= steps; i++) {
    // Multiplying (rather than adding repeatedly) keeps float noise such as 0.30000000000000004 out of the labels.
    ticks.push(Math.round((start + i * step) * 100) / 100 || 0)
  }
  return ticks
}

/** Axis labels: 300000 -> "$300K", -1250000 -> "-$1.25M", 0 -> "$0". */
export function formatAxisMoney(value: number): string {
  const sign = value < 0 ? '-' : ''
  const absolute = Math.abs(value)
  if (absolute >= 1_000_000) {
    return `${sign}$${trimZeros((absolute / 1_000_000).toFixed(2))}M`
  }
  if (absolute >= 1_000) {
    return `${sign}$${trimZeros((absolute / 1_000).toFixed(1))}K`
  }
  return `${sign}$${Math.round(absolute)}`
}

function trimZeros(text: string): string {
  return text.replace(/\.?0+$/, '')
}
