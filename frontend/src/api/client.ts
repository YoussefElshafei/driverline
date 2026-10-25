import type {
  Account,
  BudgetEntry,
  BudgetGrid,
  Department,
  ImportSummary,
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
