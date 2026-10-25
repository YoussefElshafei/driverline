import type { ScenarioComparison } from '../../api/types'
import { formatPercent, formatSignedMoney } from '../../lib/format'
import { formatMoney } from '../../lib/money'
import { FavorabilityBadge } from '../variance/FavorabilityBadge'

interface ScenarioCardsProps {
  comparison: ScenarioComparison
  selected: string
  onSelect: (scenarioCode: string) => void
}

/** Full-year net income for each scenario against budget. Clicking a card shows that scenario's detail. */
export function ScenarioCards({
  comparison,
  selected,
  onSelect,
}: ScenarioCardsProps) {
  return (
    <div className="summary-cards">
      {comparison.scenarios.map(({ scenarioCode, scenarioName, summary }) => (
        <button
          key={scenarioCode}
          type="button"
          className={`summary-card scenario-card${scenarioCode === selected ? ' scenario-card-selected' : ''}`}
          aria-pressed={scenarioCode === selected}
          aria-label={`${scenarioName} scenario`}
          onClick={() => onSelect(scenarioCode)}
        >
          <h3>{scenarioName}</h3>
          <p className="summary-actual">
            {formatMoney(summary.netIncome.outlook)}
          </p>
          <p className="summary-budget">
            Net income outlook · budget {formatMoney(summary.netIncome.budget)}
          </p>
          <p className="summary-variance">
            {formatSignedMoney(summary.netIncome.variance)} (
            {formatPercent(summary.netIncome.variancePercent)}){' '}
            <FavorabilityBadge favorability={summary.netIncome.favorability} />
          </p>
          <p className="summary-budget">
            Revenue {formatMoney(summary.revenue.outlook)} · Expenses{' '}
            {formatMoney(summary.expenses.outlook)}
          </p>
        </button>
      ))}
    </div>
  )
}
