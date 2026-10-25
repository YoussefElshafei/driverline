import { useEffect, useState } from 'react'
import { forecastApi, forecastExportUrl } from '../../api/client'
import type {
  DriverGrid,
  ForecastReport,
  ScenarioComparison,
} from '../../api/types'
import { monthName } from '../../lib/months'
import { DriverEditor } from './DriverEditor'
import { ForecastTable } from './ForecastTable'
import { ScenarioCards } from './ScenarioCards'

const YEARS = [2026, 2027]
const DEFAULT_YEAR = 2026

interface Loaded<T> {
  key: string
  data: T
}

export function ForecastPage() {
  const [year, setYear] = useState(DEFAULT_YEAR)
  const [scenario, setScenario] = useState('BASE')
  // Bumped after drivers are saved, so every view reloads with the new forecast.
  const [version, setVersion] = useState(0)
  const [comparison, setComparison] =
    useState<Loaded<ScenarioComparison> | null>(null)
  const [report, setReport] = useState<Loaded<ForecastReport> | null>(null)
  const [grid, setGrid] = useState<Loaded<DriverGrid> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [savedMessage, setSavedMessage] = useState<string | null>(null)

  const yearKey = `${year}|${version}`
  const reportKey = `${year}|${scenario}|${version}`

  useEffect(() => {
    let cancelled = false
    Promise.all([forecastApi.compare(year), forecastApi.drivers(year)])
      .then(([loadedComparison, loadedGrid]) => {
        if (!cancelled) {
          setComparison({ key: yearKey, data: loadedComparison })
          setGrid({ key: yearKey, data: loadedGrid })
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

  useEffect(() => {
    let cancelled = false
    forecastApi
      .report(year, scenario)
      .then((loaded) => {
        if (!cancelled) {
          setReport({ key: reportKey, data: loaded })
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
  }, [year, scenario, reportKey])

  const latestActualMonth =
    report?.data.latestActualMonth ?? comparison?.data.latestActualMonth ?? 0

  return (
    <section aria-labelledby="forecast-heading">
      <h2 id="forecast-heading">Forecast</h2>

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
        <a className="button" href={forecastExportUrl(year, scenario)} download>
          Export to Excel
        </a>
      </div>

      {report && (
        <PeriodNote year={year} latestActualMonth={latestActualMonth} />
      )}

      {error && (
        <p role="alert" className="message message-error">
          Could not load the forecast: {error}
        </p>
      )}

      {comparison?.key === yearKey ? (
        <ScenarioCards
          comparison={comparison.data}
          selected={scenario}
          onSelect={setScenario}
        />
      ) : (
        !error && <p>Loading scenarios…</p>
      )}

      {report?.key === reportKey ? (
        <>
          <h3>{report.data.scenarioName} scenario</h3>
          <ForecastTable report={report.data} />
        </>
      ) : (
        !error && <p>Loading forecast…</p>
      )}

      {grid && (
        // Keep the current editor visible while a reload is in flight; remount only when new driver data arrives.
        <DriverEditor
          key={grid.key}
          grid={grid.data}
          onSaved={(message) => {
            setSavedMessage(message)
            setVersion((v) => v + 1)
          }}
        />
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
  if (latestActualMonth === 0) {
    return (
      <p className="hint">
        FY{year} has no actuals yet, so all 12 months are forecast.
      </p>
    )
  }
  if (latestActualMonth === 12) {
    return (
      <p className="hint">FY{year} is closed: all 12 months are actuals.</p>
    )
  }
  return (
    <p className="hint">
      FY{year} full-year outlook: actuals for January to{' '}
      {monthName(latestActualMonth)}, forecast for{' '}
      {monthName(latestActualMonth + 1)} to December (shaded).
    </p>
  )
}
