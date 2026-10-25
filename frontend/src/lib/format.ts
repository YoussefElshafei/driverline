import { formatMoney } from './money'

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
