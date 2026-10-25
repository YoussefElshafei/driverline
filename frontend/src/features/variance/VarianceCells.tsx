import type { VarianceLine } from '../../api/types'
import { formatPercent, formatSignedMoney } from '../../lib/format'
import { formatMoney } from '../../lib/money'
import { FavorabilityBadge } from './FavorabilityBadge'

export function VarianceHeaderCells() {
  return (
    <>
      <th scope="col" className="num">
        Budget
      </th>
      <th scope="col" className="num">
        Actual
      </th>
      <th scope="col" className="num">
        Variance
      </th>
      <th scope="col" className="num">
        Var. %
      </th>
      <th scope="col">Status</th>
    </>
  )
}

/** The five standard columns for one budget vs. actual line. */
export function VarianceCells({ line }: { line: VarianceLine }) {
  return (
    <>
      <td className="num">{formatMoney(line.budget)}</td>
      <td className="num">{formatMoney(line.actual)}</td>
      <td className="num">{formatSignedMoney(line.variance)}</td>
      <td className="num">{formatPercent(line.variancePercent)}</td>
      <td>
        <FavorabilityBadge favorability={line.favorability} />
      </td>
    </>
  )
}
