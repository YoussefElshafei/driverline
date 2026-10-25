import { Minus, TrendingDown, TrendingUp } from 'lucide-react'
import type { Favorability } from '../../api/types'

const LABELS: Record<Favorability, string> = {
  FAVORABLE: 'Favorable',
  UNFAVORABLE: 'Unfavorable',
  ON_BUDGET: 'On budget',
}

const ICONS = {
  FAVORABLE: TrendingUp,
  UNFAVORABLE: TrendingDown,
  ON_BUDGET: Minus,
}

/** Icon, text and colour together, so the meaning never depends on seeing the colour. */
export function FavorabilityBadge({
  favorability,
}: {
  favorability: Favorability
}) {
  const Icon = ICONS[favorability]
  return (
    <span
      className={`flag flag-${favorability.toLowerCase().replace('_', '-')}`}
    >
      <Icon size={13} strokeWidth={2.2} aria-hidden="true" />
      {LABELS[favorability]}
    </span>
  )
}
