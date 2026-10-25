import type { VarianceLine, VarianceSummary } from '../../api/types'
import { formatPercent, formatSignedMoney } from '../../lib/format'
import { formatMoney } from '../../lib/money'
import { FavorabilityBadge } from './FavorabilityBadge'

function Card({ title, line }: { title: string; line: VarianceLine }) {
  return (
    <article className="summary-card" aria-label={title}>
      <h3>{title}</h3>
      <p className="summary-actual">{formatMoney(line.actual)}</p>
      <p className="summary-budget">Budget {formatMoney(line.budget)}</p>
      <p className="summary-variance">
        {formatSignedMoney(line.variance)} (
        {formatPercent(line.variancePercent)}){' '}
        <FavorabilityBadge favorability={line.favorability} />
      </p>
    </article>
  )
}

export function SummaryCards({ summary }: { summary: VarianceSummary }) {
  return (
    <div className="summary-cards">
      <Card title="Revenue" line={summary.revenue} />
      <Card title="Expenses" line={summary.expenses} />
      <Card title="Net income" line={summary.netIncome} />
    </div>
  )
}
