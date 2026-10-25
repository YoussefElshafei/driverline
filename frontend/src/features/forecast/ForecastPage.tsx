import { CalendarRange, Download } from 'lucide-react'
import { useEffect, useState } from 'react'
import { forecastApi, forecastExportUrl, varianceApi } from '../../api/client'
import type {
  DriverGrid,
  ForecastReport,
  MonthlyVarianceReport,
  ScenarioComparison,
} from '../../api/types'
import { OutlookChart } from '../../charts/OutlookChart'
import { PageHeader } from '../../components/PageHeader'
import { ReportSkeleton } from '../../components/Skeleton'
import { monthName } from '../../lib/months'
import { DriverEditor } from './DriverEditor'
import { ForecastTable } from './ForecastTable'
import { buildOutlookSeries } from './outlookSeries'
import { ScenarioCards } from './ScenarioCards'

const YEARS = [2026, 2027]
const DEFAULT_YEAR = 2026

/** Everything the page shows for one fiscal year, loaded together so the views always agree. */
interface YearData {
  key: string
  comparison: ScenarioComparison
  grid: DriverGrid
  budgetTrend: MonthlyVarianceReport
  reports: Record<string, ForecastReport>
}

export function ForecastPage() {
  const [year, setYear] = useState(DEFAULT_YEAR)
  const [scenario, setScenario] = useState('BASE')
  // Bumped after drivers are saved, so every view reloads with the new forecast.
  const [version, setVersion] = useState(0)
  const [data, setData] = useState<YearData | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [savedMessage, setSavedMessage] = useState<string | null>(null)

  const yearKey = `${year}|${version}`

  useEffect(() => {
    let cancelled = false
    Promise.all([
      forecastApi.compare(year),
      forecastApi.drivers(year),
      varianceApi.byMonth({ year }),
    ])
      .then(async ([comparison, grid, budgetTrend]) => {
        // One report per scenario: the table shows the selected one, the chart draws the Best-Worst range.
        const loaded = await Promise.all(
          comparison.scenarios.map((s) =>
            forecastApi.report(year, s.scenarioCode),
          ),
        )
        const reports = Object.fromEntries(
          comparison.scenarios.map((s, i) => [s.scenarioCode, loaded[i]]),
        )
        if (!cancelled) {
          setData({ key: yearKey, comparison, grid, budgetTrend, reports })
          setError(null)
        }
      })
      .catch((e: unknown) => {
        if (!cancelled)
          setError(e instanceof Error ? e.message : 'Unexpected error')
      })
    return () => {
      cancelled = true
    }
  }, [year, yearKey])

  const refreshing = data !== null && data.key !== yearKey && !error
  const report = data?.reports[scenario]
  const series = data
    ? buildOutlookSeries(data.reports, scenario, data.budgetTrend)
    : null
  const latestActualMonth = data?.comparison.latestActualMonth ?? 0

  return (
    <section className="page" aria-labelledby="forecast-heading">
      <PageHeader
        id="forecast-heading"
        title="Forecast"
        description="Full-year outlook: actuals to date, then driver-based forecasts for the rest of the year."
        actions={
          <a
            className="button"
            href={forecastExportUrl(year, scenario)}
            download
          >
            <Download size={15} aria-hidden="true" />
            Export to Excel
          </a>
        }
      />

      <div className="card filter-bar">
        <div className="toolbar">
          <label>
            Fiscal year
            <select
              value={year}
              onChange={(e) => {
                setYear(Number(e.target.value))
                setSavedMessage(null)
              }}
            >
              {YEARS.map((y) => (
                <option key={y} value={y}>
                  FY{y}
                </option>
              ))}
            </select>
          </label>
          {data && (
            <PeriodNote year={year} latestActualMonth={latestActualMonth} />
          )}
        </div>
      </div>

      {error && (
        <p role="alert" className="message message-error">
          Could not load the forecast: {error}
        </p>
      )}

      {!data && !error && <ReportSkeleton label="Loading forecast" />}

      {data && (
        <div
          className={`page-stack${refreshing ? ' is-refreshing' : ''}`}
          aria-busy={refreshing}
        >
          <ScenarioCards
            comparison={data.comparison}
            selected={scenario}
            onSelect={setScenario}
          />

          {series && report && (
            <section className="card" aria-labelledby="outlook-heading">
              <div className="card-header">
                <div>
                  <h3 id="outlook-heading" className="card-title">
                    Cumulative net income
                  </h3>
                  <p className="card-subtitle">
                    FY{year} year-to-date, {report.scenarioName} scenario
                    against budget
                  </p>
                </div>
                <ul className="chart-legend">
                  <li>
                    <span
                      className="key-line key-line-series"
                      aria-hidden="true"
                    />
                    Actual
                  </li>
                  <li>
                    <span
                      className="key-line key-line-dashed"
                      aria-hidden="true"
                    />
                    Forecast
                  </li>
                  <li>
                    <span className="key-band" aria-hidden="true" />
                    Worst – Best range
                  </li>
                  <li>
                    <span className="key-line" aria-hidden="true" />
                    Budget
                  </li>
                </ul>
              </div>
              <div className="card-body">
                <OutlookChart
                  series={series}
                  scenarioName={report.scenarioName}
                />
              </div>
            </section>
          )}

          {report && (
            <section
              className="card table-card"
              aria-labelledby="scenario-detail-heading"
            >
              <div className="table-heading">
                <div>
                  <h3 id="scenario-detail-heading" className="card-title">
                    {report.scenarioName} scenario
                  </h3>
                  <p className="card-subtitle">
                    Month by month; forecast months are shaded.
                  </p>
                </div>
              </div>
              <ForecastTable report={report} />
            </section>
          )}

          {/* Remount the editor only when new driver data arrives, so it stays usable while a reload is in flight. */}
          <DriverEditor
            key={data.key}
            grid={data.grid}
            onSaved={(message) => {
              setSavedMessage(message)
              setVersion((v) => v + 1)
            }}
          />
        </div>
      )}
      {savedMessage && (
        <p role="status" className="message message-success">
          {savedMessage}
        </p>
      )}
    </section>
  )
}

function PeriodNote({
  year,
  latestActualMonth,
}: {
  year: number
  latestActualMonth: number
}) {
  let text: string
  if (latestActualMonth === 0) {
    text = `FY${year} has no actuals yet, so all 12 months are forecast.`
  } else if (latestActualMonth === 12) {
    text = `FY${year} is closed: all 12 months are actuals.`
  } else {
    text = `FY${year} full-year outlook: actuals for January to ${monthName(latestActualMonth)}, forecast for ${monthName(latestActualMonth + 1)} to December (shaded).`
  }
  return (
    <p className="note">
      <CalendarRange size={14} aria-hidden="true" />
      {text}
    </p>
  )
}
