# ADR 0010: Variance sign, percentage and favorability rules

**Status:** Accepted

## Context

Variance numbers are the core output of the app, and their meaning must be unambiguous. The same +$10,000 is good news on a revenue line and bad news on an expense line. Edge cases (zero budget, negative amounts) must not crash or mislead.

## Decision

All rules live in one class, `VarianceCalculator`:

- **Variance = actual − budget.** Positive always means "actual came in higher than planned", whatever the account type.
- **Variance % = variance ÷ |budget| × 100**, rounded half-up to one decimal, calculated once (multiply, then divide) to avoid double rounding. Dividing by the absolute budget keeps the percentage's sign equal to the variance's sign even when the budget is negative.
- **Zero budget → no percentage** (`null` in JSON, "n/a" in the UI and Excel). A percentage of nothing is undefined; showing "∞" or a huge number would mislead.
- **Favorability depends on direction, not sign.** `VarianceDirection.HIGHER_IS_BETTER` (revenue, net income) or `LOWER_IS_BETTER` (expenses), derived from the account type. Exactly equal is `ON_BUDGET`.

## Alternatives considered

- **Flip the sign for expenses** so "positive = good" everywhere. Common in some reports, but then the same column means different things on different rows, and "actual − budget" can no longer be checked by hand.
- **Percentage of actual instead of budget.** Less common in FP&A; variance against plan is conventionally expressed relative to the plan.
- **Treat a zero budget as 100%.** Hides that nothing was planned.

## Consequences

- One unit-tested place defines the rules; reports and Excel only display them.
- Users must read the Status column (or badge) to know whether a variance is good or bad. The sign alone tells them direction.
