import { CircleCheck, FileSpreadsheet, FileUp, X } from 'lucide-react'
import { type DragEvent, type FormEvent, useState } from 'react'
import { ACTUALS_TEMPLATE_URL, ApiError, api } from '../../api/client'
import type { ImportSummary, RowError } from '../../api/types'
import { PageHeader } from '../../components/PageHeader'
import { ImportErrorTable } from './ImportErrorTable'

type Outcome =
  | { kind: 'success'; summary: ImportSummary }
  | { kind: 'failure'; message: string; errors: RowError[] }

export function ActualsImportPage() {
  const [file, setFile] = useState<File | null>(null)
  const [uploading, setUploading] = useState(false)
  const [outcome, setOutcome] = useState<Outcome | null>(null)
  const [dragging, setDragging] = useState(false)

  function chooseFile(next: File | null) {
    setFile(next)
    setOutcome(null)
  }

  function handleDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    setDragging(false)
    chooseFile(event.dataTransfer.files[0] ?? null)
  }

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
    <section className="page" aria-labelledby="import-heading">
      <PageHeader
        id="import-heading"
        title="Import actuals"
        description="Upload actual revenue and spending exported from your accounting system."
      />

      <div className="import-layout">
        <form className="card card-body import-form" onSubmit={handleSubmit}>
          <label
            className={`dropzone${dragging ? ' is-dragging' : ''}`}
            onDragOver={(event) => {
              event.preventDefault()
              setDragging(true)
            }}
            onDragLeave={() => setDragging(false)}
            onDrop={handleDrop}
          >
            <span className="dropzone-icon" aria-hidden="true">
              <FileUp size={22} />
            </span>
            <span className="dropzone-title">
              Drop a .csv or .xlsx file here, or click to browse
            </span>
            <span className="hint">
              Up to 5 MB. The first row must be the header.
            </span>
            <input
              className="visually-hidden"
              type="file"
              accept=".csv,.xlsx"
              aria-label="Actuals file"
              onChange={(event) => chooseFile(event.target.files?.[0] ?? null)}
            />
          </label>
          <div className="actions">
            {file && (
              <span className="file-chip">
                <FileSpreadsheet size={14} aria-hidden="true" />
                {file.name}
                <button
                  type="button"
                  className="toggle"
                  aria-label="Remove file"
                  onClick={() => chooseFile(null)}
                >
                  <X size={13} aria-hidden="true" />
                </button>
              </span>
            )}
            <button
              type="submit"
              className="primary"
              disabled={!file || uploading}
            >
              {uploading ? 'Importing…' : 'Import'}
            </button>
          </div>
        </form>

        <aside className="card card-body format-card">
          <h3 className="card-title">File format</h3>
          <p className="hint">
            The first row must contain these columns, in any order:
          </p>
          <ul className="format-list">
            {['year', 'month', 'account_code', 'department_code', 'amount'].map(
              (column) => (
                <li key={column}>
                  <code>{column}</code>
                </li>
              ),
            )}
          </ul>
          <p className="hint">
            <CircleCheck size={14} aria-hidden="true" className="inline-icon" />{' '}
            Every row is checked first: if any row has a problem, nothing is
            imported. Re-importing a month replaces those amounts.
          </p>
          <a href={ACTUALS_TEMPLATE_URL} download>
            Download a CSV template
          </a>
        </aside>
      </div>

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
