# ADR 0008: All-or-nothing actuals import that replaces existing amounts

**Status:** Accepted

## Context

Finance teams export actuals from their accounting system and load them monthly. Files contain mistakes (typos in codes, extra decimals, wrong month). Months are also commonly re-loaded after late adjustments. Two questions: what happens to a file with some bad rows, and what happens when a month is imported twice?

## Decision

1. **Validate everything first; save only if every row is valid.** All problems in the file are collected and returned together (HTTP 422). The import runs in one database transaction.
2. **Re-import replaces.** A row whose account, department and month already have an actual updates that amount; otherwise a new actual is created ("upsert"). Actuals not mentioned in the file are left untouched.
3. **One validation path for all formats.** CSV and XLSX parsers (strategy pattern behind `ActualsFileParser`) both produce text-only `RawActualRow`s; a single `ActualRowValidator` checks them.
4. **Lenient where it's safe, strict where it matters.** Headers match in any order and case; amounts accept `1,234.56`, `$1,234.56` and `(1,234.56)`. Fractions of a cent are rejected, never rounded.

## Alternatives considered

- **Import the valid rows and skip the bad ones.** Leaves the month half-loaded, so totals look complete but aren't — the worst outcome for a variance report.
- **Reject months that already have actuals.** Safe, but forces users to delete data before every correction.
- **Delete the whole month, then insert the file.** Simple, but a file containing only one corrected department would silently wipe the others.

## Consequences

- A rejected file never changes data, so retrying is always safe.
- Users can correct and re-upload a month as often as needed.
- To remove an actual entirely, a user would need a future delete feature (or import 0.00).
