import { useState } from 'react'
import type { MonthlyVarianceReport } from '../api/types'
import { FavorabilityBadge } from '../features/variance/FavorabilityBadge'
import { formatPercent, formatSignedMoney } from '../lib/format'
import { formatMoney, toCents } from '../lib/money'
import { MONTH_LONG, MONTH_SHORT } from '../lib/months'
import { ChartTooltip, TooltipRow } from './ChartTooltip'
import { formatAxisMoney, linearScale, niceTicks } from './scale'
import { useElementWidth } from './useElementWidth'

const HEIGHT = 240
const MARGIN = { top: 12, right: 8, bottom: 28, left: 56 }
const MAX_BAR_WIDTH = 24
const RADIUS = 4

interface MonthlyChartProps {
  report: MonthlyVarianceReport
  /** Months outside this range are drawn faded, so the chart echoes the period the tables above use. */
  highlight?: { from: number; to: number }
  /** Accessible summary of what is plotted, e.g. "Net income". */
  label: string
}

/**
 * Actual per closed month as columns, budget per month as a short neutral tick. Open months show the budget tick
 * only. One y-axis, zero baseline, hover/focus tooltip per month.
 */
export function MonthlyChart({ report, highlight, label }: MonthlyChartProps) {
  const [containerRef, width] = useElementWidth<HTMLDivElement>()
  const [active, setActive] = useState<number | null>(null)

  const points = report.months.map((row) => ({
    month: row.month,
    budget: toCents(row.budget) / 100,
    actual:
      row.closed && row.variance ? toCents(row.variance.actual) / 100 : null,
    row,
  }))
  const values = points.flatMap((p) =>
    p.actual === null ? [p.budget] : [p.budget, p.actual],
  )
  const ticks = niceTicks(Math.min(...values), Math.max(...values), 4)
  const innerWidth = Math.max(width - MARGIN.left - MARGIN.right, 120)
  const columnWidth = innerWidth / 12
  const barWidth = Math.min(MAX_BAR_WIDTH, columnWidth * 0.5)
  const y = linearScale(
    [ticks[0], ticks[ticks.length - 1]],
    [HEIGHT - MARGIN.bottom, MARGIN.top],
  )
  const baseline = y(0)
  const columnCenter = (index: number) =>
    MARGIN.left + columnWidth * index + columnWidth / 2

  const activePoint = active === null ? null : points[active]

  return (
    <div className="chart" ref={containerRef}>
      <svg
        width={width}
        height={HEIGHT}
        role="group"
        aria-label={`${label}: actual vs. budget by month`}
        onPointerLeave={() => setActive(null)}
      >
        <g className="chart-grid">
          {ticks.map((tick) => (
            <line
              key={tick}
              x1={MARGIN.left}
              x2={width - MARGIN.right}
              y1={y(tick)}
              y2={y(tick)}
            />
          ))}
        </g>
        {ticks.map((tick) => (
          <text
            key={tick}
            className="chart-tick"
            x={MARGIN.left - 10}
            y={y(tick)}
            textAnchor="end"
            dominantBaseline="middle"
          >
            {formatAxisMoney(tick)}
          </text>
        ))}

        {points.map((point, index) => {
          const cx = columnCenter(index)
          const faded =
            highlight !== undefined &&
            (point.month < highlight.from || point.month > highlight.to)
          return (
            <g
              key={point.month}
              className={`chart-column${active === index ? ' is-active' : ''}`}
              tabIndex={0}
              role="img"
              aria-label={describe(point.row)}
              opacity={faded ? 0.35 : 1}
              onPointerEnter={() => setActive(index)}
              onFocus={() => setActive(index)}
              onBlur={() => setActive(null)}
            >
              <rect
                className="chart-band-hover"
                x={cx - columnWidth / 2 + 2}
                y={MARGIN.top}
                width={columnWidth - 4}
                height={HEIGHT - MARGIN.top - MARGIN.bottom}
                rx={6}
              />
              {point.actual !== null && (
                <path
                  className="chart-bar"
                  d={barPath(
                    cx - barWidth / 2,
                    barWidth,
                    baseline,
                    y(point.actual),
                  )}
                />
              )}
              <line
                className="chart-reference"
                x1={cx - barWidth / 2 - 4}
                x2={cx + barWidth / 2 + 4}
                y1={y(point.budget)}
                y2={y(point.budget)}
              />
              <text
                className="chart-tick"
                x={cx}
                y={HEIGHT - 8}
                textAnchor="middle"
              >
                {MONTH_SHORT[index]}
              </text>
            </g>
          )
        })}

        <line
          className="chart-zero"
          x1={MARGIN.left}
          x2={width - MARGIN.right}
          y1={baseline}
          y2={baseline}
        />
      </svg>

      {activePoint && active !== null && (
        <ChartTooltip
          x={
            columnCenter(active) +
            (columnCenter(active) > width / 2 ? -1 : 1) * (columnWidth / 2)
          }
          y={HEIGHT / 2}
          chartWidth={width}
          title={`${MONTH_LONG[active]} ${report.year}`}
        >
          {activePoint.row.closed && activePoint.row.variance ? (
            <>
              <TooltipRow
                swatch={<span className="key-bar" />}
                label="Actual"
                value={formatMoney(activePoint.row.variance.actual)}
              />
              <TooltipRow
                swatch={<span className="key-line" />}
                label="Budget"
                value={formatMoney(activePoint.row.budget)}
              />
              <div className="chart-tooltip-footer chart-tooltip-row">
                <span>
                  {formatSignedMoney(activePoint.row.variance.variance)} (
                  {formatPercent(activePoint.row.variance.variancePercent)})
                </span>
                <FavorabilityBadge
                  favorability={activePoint.row.variance.favorability}
                />
              </div>
            </>
          ) : (
            <>
              <TooltipRow
                swatch={<span className="key-line" />}
                label="Budget"
                value={formatMoney(activePoint.row.budget)}
              />
              <p className="hint">Not closed yet</p>
            </>
          )}
        </ChartTooltip>
      )}
    </div>
  )
}

function describe(row: MonthlyVarianceReport['months'][number]): string {
  const month = MONTH_LONG[row.month - 1]
  if (!row.closed || !row.variance) {
    return `${month}: budget ${formatMoney(row.budget)}, not closed`
  }
  return `${month}: actual ${formatMoney(row.variance.actual)}, budget ${formatMoney(row.budget)}`
}

/** A column from the baseline to the value, rounded only at the data end (top for positive, bottom for negative). */
function barPath(x: number, width: number, base: number, top: number): string {
  const height = Math.abs(base - top)
  if (height < 0.5) {
    return ''
  }
  const r = Math.min(RADIUS, height, width / 2)
  const d = top < base ? 1 : -1
  return [
    `M${x},${base}`,
    `V${top + d * r}`,
    `Q${x},${top} ${x + r},${top}`,
    `H${x + width - r}`,
    `Q${x + width},${top} ${x + width},${top + d * r}`,
    `V${base}`,
    'Z',
  ].join(' ')
}
