import { type KeyboardEvent, type PointerEvent, useState } from 'react'
import type { OutlookSeries } from '../features/forecast/outlookSeries'
import { formatWholeDollars, fromCents } from '../lib/money'
import { MONTH_LONG, MONTH_SHORT } from '../lib/months'
import { ChartTooltip, TooltipRow } from './ChartTooltip'
import { formatAxisMoney, linearScale, niceTicks } from './scale'
import { useElementWidth } from './useElementWidth'

const HEIGHT = 280
const MARGIN = { top: 24, right: 112, bottom: 28, left: 60 }
const LABEL_GAP = 16

interface OutlookChartProps {
  series: OutlookSeries
  scenarioName: string
}

const dollars = (cents: number) => formatWholeDollars(fromCents(cents))

/**
 * Cumulative net income through the year: budget (neutral line), actuals (solid), the selected scenario's forecast
 * (dashed) and the Best-to-Worst range (light band). Hover or arrow keys move a crosshair across the months.
 */
export function OutlookChart({ series, scenarioName }: OutlookChartProps) {
  const [containerRef, width] = useElementWidth<HTMLDivElement>()
  const [active, setActive] = useState<number | null>(null)

  const latest = series.latestActualMonth
  const firstForecastIndex = Math.max(latest - 1, 0)
  const all = [
    ...series.budget,
    ...series.selected,
    ...series.best,
    ...series.worst,
  ]
  const ticks = niceTicks(Math.min(...all) / 100, Math.max(...all) / 100, 5)
  const innerWidth = Math.max(width - MARGIN.left - MARGIN.right, 160)
  const step = innerWidth / 12
  const x = (index: number) => MARGIN.left + step * (index + 0.5)
  const y = linearScale(
    [ticks[0] * 100, ticks[ticks.length - 1] * 100],
    [HEIGHT - MARGIN.bottom, MARGIN.top],
  )
  const plotRight = MARGIN.left + innerWidth

  const path = (values: number[], from: number, to: number) =>
    values
      .slice(from, to + 1)
      .map((v, i) => `${i === 0 ? 'M' : 'L'}${x(from + i)},${y(v)}`)
      .join(' ')

  const band =
    latest < 12
      ? `${path(series.best, firstForecastIndex, 11)} ${series.worst
          .slice(firstForecastIndex)
          .map((v, i) => [x(firstForecastIndex + i), y(v)] as const)
          .reverse()
          .map(([px, py]) => `L${px},${py}`)
          .join(' ')} Z`
      : null

  // Direct labels at the right end; nudged apart when the two lines finish close together.
  let selectedLabelY = y(series.selected[11])
  let budgetLabelY = y(series.budget[11])
  if (Math.abs(selectedLabelY - budgetLabelY) < LABEL_GAP * 2) {
    const middle = (selectedLabelY + budgetLabelY) / 2
    const selectedAbove = selectedLabelY <= budgetLabelY
    selectedLabelY = middle + (selectedAbove ? -LABEL_GAP : LABEL_GAP)
    budgetLabelY = middle + (selectedAbove ? LABEL_GAP : -LABEL_GAP)
  }

  function onPointerMove(event: PointerEvent<SVGRectElement>) {
    const bounds = event.currentTarget.getBoundingClientRect()
    const index = Math.floor(
      ((event.clientX - bounds.left) / bounds.width) * 12,
    )
    setActive(Math.min(11, Math.max(0, index)))
  }

  function onKeyDown(event: KeyboardEvent<SVGRectElement>) {
    if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') {
      event.preventDefault()
      const delta = event.key === 'ArrowRight' ? 1 : -1
      setActive((current) =>
        Math.min(11, Math.max(0, (current ?? (delta > 0 ? -1 : 12)) + delta)),
      )
    }
  }

  const isForecast = (index: number) => index + 1 > latest

  return (
    <div className="chart" ref={containerRef}>
      <svg
        width={width}
        height={HEIGHT}
        role="group"
        aria-label={`Cumulative net income for the ${scenarioName} scenario against budget, ending at ${dollars(series.selected[11])} versus a budget of ${dollars(series.budget[11])}`}
      >
        {latest < 12 && (
          <>
            <rect
              className="chart-forecast-zone"
              x={latest === 0 ? MARGIN.left : x(firstForecastIndex)}
              y={MARGIN.top - 8}
              width={
                plotRight - (latest === 0 ? MARGIN.left : x(firstForecastIndex))
              }
              height={HEIGHT - MARGIN.top - MARGIN.bottom + 8}
              rx={6}
            />
            <text
              className="chart-label-muted"
              x={(latest === 0 ? MARGIN.left : x(firstForecastIndex)) + 8}
              y={MARGIN.top + 6}
            >
              Forecast
            </text>
          </>
        )}

        <g className="chart-grid">
          {ticks.map((tick) => (
            <line
              key={tick}
              x1={MARGIN.left}
              x2={plotRight}
              y1={y(tick * 100)}
              y2={y(tick * 100)}
            />
          ))}
        </g>
        {ticks.map((tick) => (
          <text
            key={tick}
            className="chart-tick"
            x={MARGIN.left - 10}
            y={y(tick * 100)}
            textAnchor="end"
            dominantBaseline="middle"
          >
            {formatAxisMoney(tick)}
          </text>
        ))}
        {MONTH_SHORT.map((month, index) => (
          <text
            key={month}
            className={`chart-tick${active === index ? ' chart-tick-strong' : ''}`}
            x={x(index)}
            y={HEIGHT - 8}
            textAnchor="middle"
          >
            {month}
          </text>
        ))}
        <line
          className="chart-zero"
          x1={MARGIN.left}
          x2={plotRight}
          y1={y(0)}
          y2={y(0)}
        />

        {band && <path className="chart-area" d={band} />}
        <path className="chart-line-reference" d={path(series.budget, 0, 11)} />
        {latest > 0 && (
          <path
            className="chart-line"
            d={path(series.selected, 0, latest - 1)}
          />
        )}
        {latest < 12 && (
          <path
            className="chart-line chart-line-forecast"
            d={path(series.selected, firstForecastIndex, 11)}
          />
        )}
        {latest > 0 && latest < 12 && (
          <circle
            className="chart-dot"
            cx={x(latest - 1)}
            cy={y(series.selected[latest - 1])}
            r={4}
          />
        )}

        <text
          className="chart-label"
          x={plotRight + 10}
          y={selectedLabelY}
          dominantBaseline="middle"
        >
          {dollars(series.selected[11])}
        </text>
        <text
          className="chart-label-muted"
          x={plotRight + 10}
          y={selectedLabelY + 14}
          dominantBaseline="middle"
        >
          {scenarioName}
        </text>
        <text
          className="chart-label"
          x={plotRight + 10}
          y={budgetLabelY}
          dominantBaseline="middle"
        >
          {dollars(series.budget[11])}
        </text>
        <text
          className="chart-label-muted"
          x={plotRight + 10}
          y={budgetLabelY + 14}
          dominantBaseline="middle"
        >
          Budget
        </text>

        {active !== null && (
          <g pointerEvents="none">
            <line
              className="chart-crosshair"
              x1={x(active)}
              x2={x(active)}
              y1={MARGIN.top - 8}
              y2={HEIGHT - MARGIN.bottom}
            />
            <circle
              className="chart-dot-reference"
              cx={x(active)}
              cy={y(series.budget[active])}
              r={4.5}
            />
            <circle
              className="chart-dot"
              cx={x(active)}
              cy={y(series.selected[active])}
              r={4.5}
            />
          </g>
        )}

        <rect
          className="chart-overlay"
          x={MARGIN.left}
          y={MARGIN.top - 8}
          width={innerWidth}
          height={HEIGHT - MARGIN.top - MARGIN.bottom + 8}
          fill="transparent"
          tabIndex={0}
          aria-label="Explore months with the left and right arrow keys"
          onPointerMove={onPointerMove}
          onPointerLeave={() => setActive(null)}
          onKeyDown={onKeyDown}
          onBlur={() => setActive(null)}
        />
      </svg>

      {active !== null && (
        <ChartTooltip
          x={x(active)}
          y={HEIGHT / 2}
          chartWidth={width}
          title={`Through ${MONTH_LONG[active]} · ${isForecast(active) ? 'forecast' : 'actual'}`}
        >
          <TooltipRow
            swatch={
              <span
                className={`key-line key-line-series${isForecast(active) ? ' key-line-dashed' : ''}`}
              />
            }
            label={isForecast(active) ? scenarioName : 'Actual'}
            value={dollars(series.selected[active])}
          />
          <TooltipRow
            swatch={<span className="key-line" />}
            label="Budget"
            value={dollars(series.budget[active])}
          />
          {isForecast(active) && (
            <TooltipRow
              swatch={<span className="key-band" />}
              label="Worst – Best"
              value={`${dollars(series.worst[active])} – ${dollars(series.best[active])}`}
            />
          )}
        </ChartTooltip>
      )}
    </div>
  )
}
