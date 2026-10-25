import type {
  Account,
  AccountVarianceReport,
  BudgetEntry,
  BudgetGrid,
  Department,
  DepartmentVarianceReport,
  ImportSummary,
  MonthlyVarianceReport,
  ProblemDetail,
  SaveBudgetResult,
} from './types'

/** Thrown for any non-2xx response. Carries the server's problem details when it sent them. */
export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail | null

  constructor(status: number, problem: ProblemDetail | null) {
    super(
      problem?.detail ?? problem?.title ?? `Request failed (HTTP ${status})`,
    )
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init)
  if (!response.ok) {
    throw new ApiError(response.status, await readProblem(response))
  }
  return (await response.json()) as T
}

async function readProblem(response: Response): Promise<ProblemDetail | null> {
  try {
    return (await response.json()) as ProblemDetail
  } catch {
    return null
  }
}

export const api = {
  listAccounts: () => request<Account[]>('/api/accounts'),

  listDepartments: () => request<Department[]>('/api/departments'),

  getBudgetGrid: (year: number, departmentCode?: string) => {
    const params = new URLSearchParams({ year: String(year) })
    if (departmentCode) {
      params.set('department', departmentCode)
    }
    return request<BudgetGrid>(`/api/budgets?${params}`)
  },

  saveBudget: (year: number, entries: BudgetEntry[]) =>
    request<SaveBudgetResult>('/api/budgets', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ year, entries }),
    }),

  importActuals: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    // No Content-Type header: the browser sets multipart/form-data with the correct boundary itself.
    return request<ImportSummary>('/api/actuals/import', {
      method: 'POST',
      body: form,
    })
  },
}

export const ACTUALS_TEMPLATE_URL = '/api/actuals/import/template'

export interface PeriodQuery {
  year: number
  /** Omit to let the server default to January */
  fromMonth?: number
  /** Omit to let the server default to the latest month with actuals */
  toMonth?: number
}

export interface MonthlyQuery {
  year: number
  department?: string
  account?: string
}

/** Builds a query string, leaving out parameters that are undefined. */
function queryString(params: object): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') {
      search.set(key, String(value))
    }
  }
  return search.toString()
}

export const varianceApi = {
  byDepartment: (query: PeriodQuery) =>
    request<DepartmentVarianceReport>(
      `/api/variance/by-department?${queryString(query)}`,
    ),
  byAccount: (query: PeriodQuery) =>
    request<AccountVarianceReport>(
      `/api/variance/by-account?${queryString(query)}`,
    ),
  byMonth: (query: MonthlyQuery) =>
    request<MonthlyVarianceReport>(
      `/api/variance/by-month?${queryString(query)}`,
    ),
}

/** Plain links: the browser downloads the .xlsx itself, using the file name the server sends. */
export const varianceExportUrl = {
  byDepartment: (query: PeriodQuery) =>
    `/api/variance/by-department/export?${queryString(query)}`,
  byAccount: (query: PeriodQuery) =>
    `/api/variance/by-account/export?${queryString(query)}`,
  byMonth: (query: MonthlyQuery) =>
    `/api/variance/by-month/export?${queryString(query)}`,
}
