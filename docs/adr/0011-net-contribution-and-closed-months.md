# ADR 0011: Department totals as net contribution, and reporting only closed months by default

**Status:** Accepted

## Context

Two questions shape every report:

1. **What is a department's total?** Departments mix revenue and expense accounts. Adding revenue and expenses together is meaningless.
2. **Which months should be compared?** FY2026 has budgets for all 12 months but actuals only through September. Comparing a full-year budget with nine months of actuals makes every expense look hugely favorable.

## Decision

- **Department total = net contribution = revenue − expenses** (each account's `profitImpact`). Higher is better, so favorability uses `HIGHER_IS_BETTER`. Department contributions add up exactly to company net income. Drilling into a department shows its accounts in their natural sign, with type-aware favorability.
- **Months are selectable** (`fromMonth`, `toMonth`). The default is January through the latest month with any actuals ("closed months"). Reports return `latestActualMonth`, so the UI can warn when a chosen range goes past it.
- **Within the selected range, a missing actual counts as zero.** If a month is closed and an account has nothing recorded, nothing was earned or spent.
- **The monthly trend marks months without actuals as "not closed"** and shows only their budget. Its total covers closed months only.
- Facts for a year are loaded with two queries and aggregated in memory with `BigDecimal` (`VarianceReportBuilder`, pure and unit-tested).

## Alternatives considered

- **Show revenue and expense columns per department** instead of a net figure. Clearer for mixed departments but doubles the table width, and totals no longer roll up to one number.
- **Always compare the full year.** Simple, but systematically misleading mid-year.
- **Aggregate in SQL** (`GROUP BY`). Faster for large data sets, but spreads the rules across queries and Java. At a few hundred lines per year, in-memory aggregation is simpler to test. It would be revisited if data volumes grew by orders of magnitude.

## Consequences

- A cost centre (no revenue) has a negative net contribution, and spending less than budget shows as a favorable variance. The UI and Excel export explain this in a note.
- Every level of the drill-down reconciles: accounts → department → company.
