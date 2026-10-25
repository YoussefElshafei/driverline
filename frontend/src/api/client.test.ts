import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, api } from './client'

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('api client', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('builds the budget grid URL with an optional department filter', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        jsonResponse({ year: 2026, departmentCode: 'OPS', rows: [] }),
      )
    vi.stubGlobal('fetch', fetchMock)

    await api.getBudgetGrid(2026, 'OPS')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/budgets?year=2026&department=OPS',
      undefined,
    )
  })

  it('throws an ApiError carrying the problem details on failure', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          {
            title: 'Validation failed',
            status: 400,
            detail: 'Some budget entries are invalid. Nothing was saved.',
            errors: [
              { field: 'entries[0].month', message: 'must be at most 12' },
            ],
          },
          400,
        ),
      ),
    )

    const error = await api.saveBudget(2026, []).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(400)
    expect((error as ApiError).message).toBe(
      'Some budget entries are invalid. Nothing was saved.',
    )
    expect((error as ApiError).problem?.errors).toHaveLength(1)
  })

  it('still throws a useful ApiError when the error body is not JSON', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response('Bad Gateway', { status: 502 })),
    )

    const error = await api.listAccounts().catch((e: unknown) => e)

    expect((error as ApiError).message).toBe('Request failed (HTTP 502)')
    expect((error as ApiError).problem).toBeNull()
  })
})
