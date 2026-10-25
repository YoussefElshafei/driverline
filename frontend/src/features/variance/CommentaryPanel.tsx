import { useState } from 'react'
import { type PeriodQuery, varianceApi } from '../../api/client'
import type { VarianceCommentary } from '../../api/types'

type State =
  | { kind: 'idle' }
  | { kind: 'loading' }
  | { kind: 'done'; commentary: VarianceCommentary }
  | { kind: 'error'; message: string }

const PROVIDER_NAMES: Record<string, string> = { ollama: 'Ollama' }

/**
 * Plain-English commentary on the period's largest variances, generated on request because a local language
 * model can take a few seconds (longer on its first run). Re-mount it (via a React key) when the period changes.
 */
export function CommentaryPanel({ query }: { query: PeriodQuery }) {
  const [state, setState] = useState<State>({ kind: 'idle' })

  async function generate() {
    setState({ kind: 'loading' })
    try {
      setState({
        kind: 'done',
        commentary: await varianceApi.commentary(query),
      })
    } catch (error) {
      setState({
        kind: 'error',
        message: error instanceof Error ? error.message : 'Unexpected error',
      })
    }
  }

  return (
    <section aria-labelledby="commentary-heading" className="commentary">
      <div className="table-heading">
        <h3 id="commentary-heading">AI commentary</h3>
        <button
          type="button"
          className="primary"
          onClick={generate}
          disabled={state.kind === 'loading'}
        >
          {state.kind === 'done' ? 'Regenerate' : 'Generate commentary'}
        </button>
      </div>

      {state.kind === 'idle' && (
        <p className="hint">
          Explains the largest variances for this period in plain English, using
          a local language model (Ollama) when it is running, or a rule-based
          summary otherwise.
        </p>
      )}
      {state.kind === 'loading' && (
        <p role="status" className="hint">
          Writing commentary… a local model can take up to a minute the first
          time it loads.
        </p>
      )}
      {state.kind === 'error' && (
        <p role="alert" className="message message-error">
          {state.message}
        </p>
      )}
      {state.kind === 'done' && (
        <>
          <p className="commentary-period">{state.commentary.periodLabel}</p>
          <div className="commentary-text">{state.commentary.text}</div>
          <p className="hint commentary-source">
            {sourceText(state.commentary)}
          </p>
        </>
      )}
    </section>
  )
}

function sourceText(commentary: VarianceCommentary): string {
  if (commentary.provider === 'rule-based') {
    return commentary.fallbackReason
      ? `Rule-based summary. AI commentary unavailable: ${commentary.fallbackReason}.`
      : 'Rule-based summary.'
  }
  const provider = PROVIDER_NAMES[commentary.provider] ?? commentary.provider
  return `Written by ${commentary.model} via ${provider}. Figures were checked against the report; review the wording before sharing.`
}
