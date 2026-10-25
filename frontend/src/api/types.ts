/** Types mirroring planning-service's JSON. Money is always a string such as "1234.50". */

export type AccountType = 'REVENUE' | 'EXPENSE'

export interface Account {
  code: string
  name: string
  type: AccountType
}

export interface Department {
  code: string
  name: string
}

export interface BudgetGridRow {
  accountCode: string
  accountName: string
  accountType: AccountType
  departmentCode: string
  departmentName: string
  /** 12 entries, January first; null where no budget has been entered */
  months: (string | null)[]
  total: string
}

export interface BudgetGrid {
  year: number
  departmentCode: string | null
  rows: BudgetGridRow[]
}

export interface BudgetEntry {
  accountCode: string
  departmentCode: string
  month: number
  amount: string
}

export interface SaveBudgetResult {
  year: number
  created: number
  updated: number
}

export interface ImportSummary {
  fileName: string
  rowsImported: number
  created: number
  updated: number
  periods: string[]
}

export interface FieldError {
  field: string
  message: string
}

export interface RowError {
  /** Row number as shown in Excel (header is row 1); null for whole-file problems */
  row: number | null
  column: string | null
  value: string | null
  message: string
}

/** RFC 9457 problem details, as returned by every planning-service error response. */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  errors?: (FieldError | RowError)[]
}
