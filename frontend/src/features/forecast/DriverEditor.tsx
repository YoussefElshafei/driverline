import { useState } from 'react'
import { ApiError, forecastApi } from '../../api/client'
import type {
  DriverGrid,
  DriverLine,
  FieldError,
  ForecastMethod,
} from '../../api/types'
import { formatMoney } from '../../lib/money'
import {
  type LineDraft,
  allowedMethods,
  draftFrom,
  isChanged,
  lineKey,
  monthlyPreview,
  toRequest,
  validateDraft,
} from './driverEdits'

const METHOD_OPTIONS: Record<ForecastMethod, string> = {
  UNITS_X_RATE: 'Units × rate',
  PERCENT_OF_REVENUE: '% of revenue',
  BUDGET: 'Use budget',
}

interface DriverEditorProps {
  grid: DriverGrid
  /** Called with a confirmation message after a successful save; the page reloads the forecast. */
  onSaved: (message: string) => void
}

type SaveError = { text: string; details: string[] }

/**
 * Edit each line's forecast method and its drivers for every scenario, side by side.
 * Re-mount (via a React key) to reset when the year changes.
 */
export function DriverEditor({ grid, onSaved }: DriverEditorProps) {
  const scenarioCodes = grid.scenarios.map((s) => s.code)
  const [originals] = useState(() => draftsFor(grid.lines, scenarioCodes))
  const [drafts, setDrafts] = useState(originals)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<SaveError | null>(null)

  const changedLines = grid.lines.filter((line) =>
    isChanged(drafts[lineKey(line)], originals[lineKey(line)], scenarioCodes),
  )
  const errorsByLine = Object.fromEntries(
    grid.lines.map((line) => [
      lineKey(line),
      validateDraft(drafts[lineKey(line)], scenarioCodes),
    ]),
  )
  const hasErrors = changedLines.some(
    (line) => Object.keys(errorsByLine[lineKey(line)]).length > 0,
  )

  function update(line: DriverLine, change: (draft: LineDraft) => LineDraft) {
    setDrafts((current) => ({
      ...current,
      [lineKey(line)]: change(current[lineKey(line)]),
    }))
    setSaveError(null)
  }

  function setValue(
    line: DriverLine,
    scenario: string,
    field: 'quantity' | 'rate' | 'percent',
    text: string,
  ) {
    update(line, (draft) => ({
      ...draft,
      values: {
        ...draft.values,
        [scenario]: { ...draft.values[scenario], [field]: text },
      },
    }))
  }

  async function save() {
    setSaving(true)
    setSaveError(null)
    const requests = changedLines.map((line) =>
      toRequest(line, drafts[lineKey(line)], scenarioCodes),
    )
    try {
      await forecastApi.saveDrivers(grid.year, requests)
      onSaved(
        `Saved drivers for ${requests.length} line${requests.length === 1 ? '' : 's'}.`,
      )
    } catch (error) {
      const details =
        error instanceof ApiError && error.problem?.errors
          ? (error.problem.errors as FieldError[]).map(
              (e) => `${e.field}: ${e.message}`,
            )
          : []
      setSaveError({
        text: error instanceof Error ? error.message : 'Unexpected error',
        details,
      })
    } finally {
      setSaving(false)
    }
  }

  return (
    <section aria-labelledby="drivers-heading" className="drivers">
      <h3 id="drivers-heading">Drivers</h3>
      <p className="hint">
        Set how each line is forecast for the open months, with values for every
        scenario. Units × rate multiplies a quantity by a monthly rate; % of
        revenue follows each month&apos;s forecast revenue; other lines use
        their budget.
      </p>
      <div className="table-scroll">
        <table className="report drivers-table">
          <thead>
            <tr>
              <th scope="col">Line</th>
              <th scope="col">Method</th>
              {grid.scenarios.map((scenario) => (
                <th key={scenario.code} scope="col">
                  {scenario.name}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {grid.lines.map((line) => {
              const key = lineKey(line)
              const draft = drafts[key]
              const errors = errorsByLine[key]
              const label = `${line.accountName}, ${line.departmentName}`
              return (
                <tr
                  key={key}
                  className={
                    changedLines.includes(line) ? 'row-changed' : undefined
                  }
                >
                  <th scope="row">
                    {line.accountCode} {line.accountName}
                    <span className="hint"> · {line.departmentName}</span>
                  </th>
                  <td>
                    <select
                      aria-label={`Method for ${label}`}
                      value={draft.method}
                      onChange={(e) =>
                        update(line, (d) => ({
                          ...d,
                          method: e.target.value as ForecastMethod,
                        }))
                      }
                    >
                      {allowedMethods(line.accountType).map((method) => (
                        <option key={method} value={method}>
                          {METHOD_OPTIONS[method]}
                        </option>
                      ))}
                    </select>
                  </td>
                  {grid.scenarios.map((scenario) => {
                    const value = draft.values[scenario.code]
                    const prefix = `${label}, ${scenario.name}`
                    if (draft.method === 'BUDGET') {
                      return (
                        <td key={scenario.code} className="hint">
                          {line.averageOpenMonthBudget
                            ? `Budget (avg ${formatMoney(line.averageOpenMonthBudget)}/mo)`
                            : 'Budget'}
                        </td>
                      )
                    }
                    if (draft.method === 'PERCENT_OF_REVENUE') {
                      return (
                        <td key={scenario.code}>
                          <DriverInput
                            label={`${prefix}, percent of revenue`}
                            value={value.percent}
                            error={errors[`${scenario.code}.percent`]}
                            onChange={(text) =>
                              setValue(line, scenario.code, 'percent', text)
                            }
                          />
                          <span className="hint">% of revenue</span>
                        </td>
                      )
                    }
                    const preview = monthlyPreview(value)
                    return (
                      <td key={scenario.code} className="driver-cell">
                        <DriverInput
                          label={`${prefix}, ${line.quantityLabel ?? 'quantity'}`}
                          value={value.quantity}
                          error={errors[`${scenario.code}.quantity`]}
                          onChange={(text) =>
                            setValue(line, scenario.code, 'quantity', text)
                          }
                          narrow
                        />
                        <span aria-hidden="true">×</span>
                        <DriverInput
                          label={`${prefix}, ${line.rateLabel ?? 'rate'}`}
                          value={value.rate}
                          error={errors[`${scenario.code}.rate`]}
                          onChange={(text) =>
                            setValue(line, scenario.code, 'rate', text)
                          }
                        />
                        <span
                          className="preview"
                          data-testid={`preview-${key}-${scenario.code}`}
                        >
                          {preview ? `= ${formatMoney(preview)}/mo` : ''}
                        </span>
                      </td>
                    )
                  })}
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      <div className="actions">
        <button
          type="button"
          className="primary"
          onClick={save}
          disabled={saving || hasErrors || changedLines.length === 0}
        >
          {saving
            ? 'Saving…'
            : `Save drivers${changedLines.length ? ` (${changedLines.length})` : ''}`}
        </button>
        <button
          type="button"
          onClick={() => {
            setDrafts(originals)
            setSaveError(null)
          }}
          disabled={saving || changedLines.length === 0}
        >
          Discard changes
        </button>
        {hasErrors && (
          <span className="field-error">
            Fix the highlighted drivers before saving.
          </span>
        )}
      </div>
      {saveError && (
        <div role="alert" className="message message-error">
          <p>{saveError.text}</p>
          {saveError.details.length > 0 && (
            <ul>
              {saveError.details.map((detail) => (
                <li key={detail}>{detail}</li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  )
}

function DriverInput({
  label,
  value,
  error,
  onChange,
  narrow = false,
}: {
  label: string
  value: string
  error: string | undefined
  onChange: (text: string) => void
  narrow?: boolean
}) {
  return (
    <input
      className={`cell driver-input${narrow ? ' driver-input-narrow' : ''}`}
      inputMode="decimal"
      aria-label={label}
      aria-invalid={error !== undefined}
      title={error}
      value={value}
      onChange={(e) => onChange(e.target.value)}
    />
  )
}

function draftsFor(
  lines: DriverLine[],
  scenarioCodes: string[],
): Record<string, LineDraft> {
  return Object.fromEntries(
    lines.map((line) => [lineKey(line), draftFrom(line, scenarioCodes)]),
  )
}
