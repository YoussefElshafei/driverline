import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { CommentaryPanel } from './CommentaryPanel'

function respondWith(body: unknown, status = 200) {
  const fetchMock = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

const PERIOD = { year: 2026, fromMonth: 1, toMonth: 9 }

describe('CommentaryPanel', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('only calls the commentary service when asked', async () => {
    const fetchMock = respondWith({})
    render(<CommentaryPanel query={PERIOD} />)

    expect(
      screen.getByRole('button', { name: 'Generate commentary' }),
    ).toBeEnabled()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('shows language model commentary and who wrote it', async () => {
    const fetchMock = respondWith({
      year: 2026,
      fromMonth: 1,
      toMonth: 9,
      periodLabel: 'FY2026, January to September',
      text: 'Revenue fell short by $53,962.\n\n- Vehicle & Fuel was $14,454 over budget.',
      provider: 'ollama',
      model: 'llama3.1:8b',
      fallbackReason: null,
    })
    const user = userEvent.setup()
    render(<CommentaryPanel query={PERIOD} />)

    await user.click(
      screen.getByRole('button', { name: 'Generate commentary' }),
    )

    expect(
      await screen.findByText(/Revenue fell short by \$53,962/),
    ).toBeInTheDocument()
    expect(screen.getByText('FY2026, January to September')).toBeInTheDocument()
    expect(
      screen.getByText(/Written by llama3.1:8b via Ollama/),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Regenerate' }),
    ).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/variance/commentary?year=2026&fromMonth=1&toMonth=9',
      undefined,
    )
  })

  it('explains when a rule-based summary was used instead of the language model', async () => {
    respondWith({
      year: 2026,
      fromMonth: 1,
      toMonth: 9,
      periodLabel: 'FY2026, January to September',
      text: 'FY2026, January to September: net income was $209,264 ...',
      provider: 'rule-based',
      model: null,
      fallbackReason: 'Could not reach Ollama at http://localhost:11434',
    })
    const user = userEvent.setup()
    render(<CommentaryPanel query={PERIOD} />)

    await user.click(
      screen.getByRole('button', { name: 'Generate commentary' }),
    )

    expect(
      await screen.findByText(
        'Rule-based summary. AI commentary unavailable: Could not reach Ollama at http://localhost:11434.',
      ),
    ).toBeInTheDocument()
  })

  it('shows the error when the commentary service is down', async () => {
    respondWith(
      {
        title: 'Commentary unavailable',
        status: 503,
        detail:
          'The commentary service is not reachable. Start commentary-service and try again.',
      },
      503,
    )
    const user = userEvent.setup()
    render(<CommentaryPanel query={PERIOD} />)

    await user.click(
      screen.getByRole('button', { name: 'Generate commentary' }),
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The commentary service is not reachable.',
    )
  })
})
