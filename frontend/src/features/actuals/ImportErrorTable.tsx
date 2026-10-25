import type { RowError } from '../../api/types'

interface ImportErrorTableProps {
  errors: RowError[]
}

/** Lists every problem the server found, so the user can fix the whole file in one pass. */
export function ImportErrorTable({ errors }: ImportErrorTableProps) {
  return (
    <div className="table-scroll">
      <table className="errors-table">
        <thead>
          <tr>
            <th scope="col">Row</th>
            <th scope="col">Column</th>
            <th scope="col">Value</th>
            <th scope="col">Problem</th>
          </tr>
        </thead>
        <tbody>
          {errors.map((error, index) => (
            <tr key={`${error.row}-${error.column}-${index}`}>
              <td>{error.row ?? 'File'}</td>
              <td>{error.column ?? '—'}</td>
              <td>{error.value ? <code>{error.value}</code> : '—'}</td>
              <td>{error.message}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
