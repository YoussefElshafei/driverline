import type { ReactNode } from 'react'

/**
 * A floating card beside the hovered point, in the chart container's pixel space. It sits to the right of `x`, or to
 * the left once past the middle of the chart, so it never runs off the edge or covers the hovered mark.
 */
export function ChartTooltip({
  x,
  y,
  chartWidth,
  title,
  children,
}: {
  x: number
  y: number
  chartWidth: number
  title: string
  children: ReactNode
}) {
  const side = x > chartWidth / 2 ? 'left' : 'right'
  return (
    <div
      className={`chart-tooltip chart-tooltip-${side}`}
      style={{ left: x, top: y }}
      aria-hidden="true"
    >
      <p className="chart-tooltip-title">{title}</p>
      {children}
    </div>
  )
}

export function TooltipRow({
  swatch,
  label,
  value,
}: {
  swatch?: ReactNode
  label: string
  value: string
}) {
  return (
    <div className="chart-tooltip-row">
      <span>
        {swatch}
        {label}
      </span>
      <strong>{value}</strong>
    </div>
  )
}
