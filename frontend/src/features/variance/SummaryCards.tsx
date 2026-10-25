import { CircleDollarSign, Receipt, Wallet } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { VarianceLine, VarianceSummary } from '../../api/types'
import { formatPercent, formatSignedMoney } from '../../lib/format'
import { formatMoney } from '../../lib/money'
import { FavorabilityBadge } from './FavorabilityBadge'

function Card({
  title,
  icon: Icon,
  line,
}: {
  title: string
  icon: LucideIcon
  line: VarianceLine
}) {
  return (
    <article className="summary-card" aria-label={title}>
      <h3>
        <Icon size={15} strokeWidth={1.9} aria-hidden="true" />
        {title}
      </h3>
      <p className="summary-actual">{formatMoney(line.actual)}</p>
      <p className="summary-budget">Budget {formatMoney(line.budget)}</p>
      <p className="summary-variance">
        <span>
          {formatSignedMoney(line.variance)} (
          {formatPercent(line.variancePercent)})
        </span>
        <FavorabilityBadge favorability={line.favorability} />
      </p>
    </article>
  )
}

export function SummaryCards({ summary }: { summary: VarianceSummary }) {
  return (
    <div className="summary-cards">
      <Card title="Revenue" icon={CircleDollarSign} line={summary.revenue} />
      <Card title="Expenses" icon={Receipt} line={summary.expenses} />
      <Card title="Net income" icon={Wallet} line={summary.netIncome} />
    </div>
  )
}
