import { type FormEvent, useState } from 'react'
import { ACTUALS_TEMPLATE_URL, ApiError, api } from '../../api/client'
import type { ImportSummary, RowError } from '../../api/types'
import { ImportErrorTable } from './ImportErrorTable'

type Outcome =
  | { kind: 'success'; summary: ImportSummary }
  | { kind: 'failure'; message: string; errors: RowError[] }

export function ActualsImportPage() {
  const [file, setFile] = useState<File | null>(null)
  const [uploading, setUploading] = useState(false)
  const [outcome, setOutcome] = useState<Outcome | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!file) {
      return
    }
    setUploading(true)
    setOutcome(null)
    try {
      setOutcome({ kind: 'success', summary: await api.importActuals(file) })
    } catch (error) {
      setOutcome({
        kind: 'failure',
        message: error instanceof Error ? error.message : 'Unexpected error',
        errors:
          error instanceof ApiError
            ? ((error.problem?.errors as RowError[] | undefined) ?? [])
            : [],
      })
    } finally {
      setUploading(false)
    }
  }

  return (
    <section aria-labelledby="import-heading">
      <h2 id="import-heading">Import actuals</h2>
      <p className="hint">
        Upload actual revenue and spending exported from your accounting system,
        as a <strong>.csv</strong> or <strong>.xlsx</strong> file. The first row
        must contain the columns{' '}
        <code>year, month, account_code, department_code, amount</code> (in any
        order). Every row is checked first: if any row has a problem, nothing is
        imported. Re-importing a month replaces those amounts.{' '}
        <a href={ACTUALS_TEMPLATE_URL} download>
          Download a CSV template
        </a>
        .
      </p>

      <form className="toolbar" onSubmit={handleSubmit}>
        <label>
          Actuals file
          <input
            type="file"
            accept=".csv,.xlsx"
            onChange={(event) => {
              setFile(event.target.files?.[0] ?? null)
              setOutcome(null)
            }}
          />
        </label>
        <button type="submit" className="primary" disabled={!file || uploading}>
          {uploading ? 'Importing…' : 'Import'}
        </button>
      </form>

      {outcome?.kind === 'success' && (
        <div role="status" className="message message-success">
          <p>
            Imported {outcome.summary.rowsImported} row
            {outcome.summary.rowsImported === 1 ? '' : 's'} from{' '}
            <strong>{outcome.summary.fileName}</strong>:{' '}
            {outcome.summary.created} new, {outcome.summary.updated} updated.
          </p>
          <p>Periods: {outcome.summary.periods.join(', ')}</p>
        </div>
      )}

      {outcome?.kind === 'failure' && (
        <div role="alert" className="message message-error">
          <p>{outcome.message}</p>
          {outcome.errors.length > 0 && (
            <ImportErrorTable errors={outcome.errors} />
          )}
        </div>
      )}
    </section>
  )
}
