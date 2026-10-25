import type { Favorability } from '../../api/types'

const LABELS: Record<Favorability, string> = {
  FAVORABLE: 'Favorable',
  UNFAVORABLE: 'Unfavorable',
  ON_BUDGET: 'On budget',
}

/** Text plus colour, so the meaning doesn't depend on seeing the colour. */
export function FavorabilityBadge({
  favorability,
}: {
  favorability: Favorability
}) {
  return (
    <span
      className={`flag flag-${favorability.toLowerCase().replace('_', '-')}`}
    >
      {LABELS[favorability]}
    </span>
  )
}
