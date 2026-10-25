# ADR 0007: RFC 9457 problem details for every error response

**Status:** Accepted

## Context

The frontend needs to show useful errors: which field or which file row is wrong, and why. Without a convention, each endpoint tends to invent its own error JSON, and Spring's built-in errors (malformed JSON, missing parameters) look different again.

## Decision

- Every error response uses the RFC 9457 "problem details" format (`application/problem+json`): `type`, `title`, `status`, `detail`, `instance`.
- Where specific inputs are at fault, an `errors` array is added:
  - request validation: `{ "field": "entries[2].amount", "message": "..." }`
  - file import: `{ "row": 7, "column": "amount", "value": "12.345", "message": "..." }`
- A single `@RestControllerAdvice` (`GlobalExceptionHandler`) extends Spring's `ResponseEntityExceptionHandler`, so Spring's own exceptions get the same shape.
- Status codes: 400 invalid request, 413 upload too large, 415 unsupported file type, 422 file understood but its content is invalid, 500 unexpected (the details are logged, never returned).

## Alternatives considered

- **A custom error envelope** (`{ "error": { "code": ..., "message": ... } }`). Works, but it's a format nobody else knows, and Spring's built-in errors would need converting by hand.
- **Returning the first error only.** Simpler, but users then fix a file one mistake at a time.

## Consequences

- The frontend has one error-handling path (`ApiError` with an optional `problem`).
- Clients can depend on the standard fields; the `errors` extension is documented here and in the OpenAPI spec.
