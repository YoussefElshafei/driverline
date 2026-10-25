# ADR 0013: Driver-based forecast calculated from actuals plus drivers

**Status:** Accepted

## Context

Finance wants to know where the year will end up, not just how it went so far. Typing forecast numbers by hand is slow and hides the reasoning ("why is October wages $157,850?"). Driver-based planning models the *causes* instead: headcount, contracts, prices.

## Decision

- **Full-year outlook = actuals for closed months + forecast for open months.** Closed months are those with any actuals (the same rule as variance reports); a missing actual in a closed month counts as zero.
- **One method per account, department and year** (`forecast_line`):
  - `UNITS_X_RATE`: quantity × rate per month, rounded half-up to cents (contracts × average contract value, cleaners × monthly cost per cleaner, vans × cost per van).
  - `PERCENT_OF_REVENUE`: percent × that month's total forecast revenue (supplies = 7.6% of revenue). Not allowed on revenue lines, because that would be circular. The engine computes it in a second pass, after revenue.
  - `BUDGET`: the month's budget. Lines without configuration use this, so fixed costs need no setup.
- **Drivers are constant across the open months.** For FY2026 that is October to December, three months, where a single value per driver is a reasonable simplification.
- **Forecasts are calculated on every request**, never stored. `ForecastEngine` (pure) takes budgets, actuals and drivers and returns 12 months per line; `ForecastReportBuilder` reuses the variance rules (ADR 0010) to compare outlook against the full-year budget.

## Alternatives considered

- **Store forecast amounts.** Faster reads, but every driver edit, actuals import or budget change would need a recalculation job, and stored numbers could silently go stale.
- **Monthly driver values** (a different quantity for each month). More realistic for strongly seasonal lines, but triples the inputs for a three-month window. A natural next step.
- **Trend / run-rate methods** (e.g. average of the last 3 months). Useful for variable costs without obvious drivers; left for later to keep the scope finished.

## Consequences

- Changing a driver, importing a month of actuals, or editing a budget updates the outlook immediately.
- Every forecast number can be explained: "62 contracts × $4,700" or "7.6% of $337,800".
- The calculation runs once per scenario per request; at a small company's scale this is milliseconds.
