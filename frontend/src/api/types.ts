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

export type Favorability = 'FAVORABLE' | 'UNFAVORABLE' | 'ON_BUDGET'

export type VarianceDirection = 'HIGHER_IS_BETTER' | 'LOWER_IS_BETTER'

/** Budget vs. actual. variance = actual - budget; variancePercent is null when the budget is zero. */
export interface VarianceLine {
  budget: string
  actual: string
  variance: string
  variancePercent: string | null
  favorability: Favorability
}

export interface VarianceSummary {
  revenue: VarianceLine
  expenses: VarianceLine
  netIncome: VarianceLine
}

export interface AccountVarianceRow {
  accountCode: string
  accountName: string
  accountType: AccountType
  line: VarianceLine
}

export interface DepartmentVarianceRow {
  departmentCode: string
  departmentName: string
  /** Revenue minus expenses for the department; higher is better */
  netContribution: VarianceLine
  accounts: AccountVarianceRow[]
}

export interface ReportPeriodInfo {
  year: number
  fromMonth: number
  toMonth: number
  /** Last month of the year with any actuals; 0 when there are none yet */
  latestActualMonth: number
}

export interface DepartmentVarianceReport extends ReportPeriodInfo {
  summary: VarianceSummary
  departments: DepartmentVarianceRow[]
}

export interface AccountVarianceReport extends ReportPeriodInfo {
  summary: VarianceSummary
  revenue: AccountVarianceRow[]
  expenses: AccountVarianceRow[]
}

export interface MonthlyVarianceRow {
  month: number
  budget: string
  /** True when the month has actuals recorded and can be compared to budget */
  closed: boolean
  variance: VarianceLine | null
}

export interface MonthlyVarianceReport {
  year: number
  departmentCode: string | null
  accountCode: string | null
  scope: string
  direction: VarianceDirection
  latestActualMonth: number
  months: MonthlyVarianceRow[]
  closedMonthsTotal: VarianceLine
}
