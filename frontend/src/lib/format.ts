import { formatMoney, formatWholeDollars } from './money'

/** "4.4" -> "+4.4%", "-2.5" -> "-2.5%", null (zero budget) -> "n/a". */
export function formatPercent(percent: string | null): string {
  if (percent === null) {
    return 'n/a'
  }
  const isPositive = !percent.startsWith('-') && /[1-9]/.test(percent)
  return `${isPositive ? '+' : ''}${percent}%`
}

/** Shows the direction of a variance explicitly: "+$80.00", "-$25.00", "$0.00". */
export function formatSignedMoney(amount: string): string {
  const formatted = formatMoney(amount)
  const isPositive = !formatted.startsWith('-') && /[1-9]/.test(formatted)
  return isPositive ? `+${formatted}` : formatted
}

/** Whole-dollar version for dense tables: "152880.40" -> "+$152,880", "-0.40" -> "$0". */
export function formatSignedWholeDollars(amount: string): string {
  const formatted = formatWholeDollars(amount)
  const isPositive = !formatted.startsWith('-') && /[1-9]/.test(formatted)
  return isPositive ? `+${formatted}` : formatted
}
