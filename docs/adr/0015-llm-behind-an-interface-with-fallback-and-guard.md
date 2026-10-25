# ADR 0015: Language model behind an interface, with a rule-based fallback and an output guard

**Status:** Accepted

## Context

Commentary must explain the largest variances in plain English. A local language model (Ollama, `llama3.1:8b`, running on an 8 GB GPU) writes natural text, but it may be unavailable (not installed, not running, still loading), slow, or wrong. A language model can state figures that appear nowhere in the data, which in a finance report is worse than no commentary. The app and every test must work with no language model at all.

## Decision

- **`LanguageModel` interface** (`provider()`, `model()`, `generate(system, user)`), with one implementation today: `OllamaLanguageModel`, which calls Ollama's `POST /api/chat` (non-streaming, temperature 0.2). `commentary.llm.provider` selects it (`ollama`) or turns it off (`none`). A new provider is one more class and one conditional bean, with no change to the commentary logic.
- **`RuleBasedCommentaryWriter`** writes deterministic commentary from the numbers. It is used whenever the language model is off, unreachable, times out, errors, or fails the guard. The response always says who wrote the text (`provider`, `model`) and, for a fallback, why (`fallbackReason`).
- **Prompting for accuracy:** figures are given pre-formatted ("$44,059") with the direction in words ("below budget, unfavorable"), and the model is told to copy figures exactly and not invent causes.
- **`OutputGuard`** checks every answer: it strips `<think>` reasoning tags, rejects blank or overly long text, and rejects any number of 1,000 or more that doesn't match an input amount (to the dollar or cent) or the fiscal year.
- **Timeouts:** 3 s to connect (fail fast when Ollama isn't running) and 120 s to answer (the first request after idle loads the model). planning-service waits 150 s, so commentary-service's fallback always answers first.

## Alternatives considered

- **Call Ollama directly from the controller.** Simplest, but ties the service to one provider and makes the logic untestable without a running model.
- **Spring AI.** A full abstraction over many providers, but a large dependency for one HTTP call, and its API was still changing.
- **Fail with an error when the model is down.** Honest, but violates "the app works with no LLM", and the rule-based text is genuinely useful.
- **No guard; trust the prompt.** The prompt reduces mistakes but doesn't prevent them.

## Consequences

- The app always returns commentary, and users can see whether it came from AI.
- Invented dollar figures never reach users. Limitations: wording and direction errors aren't caught by the guard (the UI asks users to review wording), and small numbers such as percentages aren't checked.
- All tests run offline: the model is mocked in unit tests, Ollama's HTTP API is mocked with `MockRestServiceServer`, and integration tests run with the model off or unreachable.
