# ADR 0005: Monthly budget and actual tables keyed by account, department and period

**Status:** Accepted

## Context

Variance reports compare budget against actual by month, account and department, with drill-down from department totals to accounts. Money must be exact.

## Decision

- Two tables, `budget_line` and `actual_line`, with the same shape: `account_id`, `department_id`, `fiscal_year`, `fiscal_month`, `amount`.
- A unique constraint on `(account_id, department_id, fiscal_year, fiscal_month)` in each table.
- The period is stored as two integer columns and mapped to a `FiscalPeriod` Java record (an `@Embeddable`). The record rejects invalid months, and the database enforces `CHECK (fiscal_month BETWEEN 1 AND 12)` as a second line of defence.
- The fiscal year equals the calendar year (January start).
- `amount` is `NUMERIC(15,2)` mapped to `BigDecimal`, stored with its natural sign. `account.type` (REVENUE/EXPENSE) drives favorable/unfavorable logic.

## Alternatives considered

- **A single `ledger_line` table with a `kind` column (BUDGET/ACTUAL).** Less duplication, but every query needs a filter, and the two kinds will diverge (actuals will track their import batch, budgets may get versions).
- **A separate `fiscal_period` table.** Useful for non-calendar fiscal years or 4-4-5 calendars, but adds a join to every query for no benefit with a calendar fiscal year.
- **Storing expenses as negative numbers.** Matches some accounting systems, but finance users enter and read budgets as positive numbers, and sign-flipping is a common source of bugs.
- **`double` for money.** Rejected: binary floating point can't represent most decimal amounts exactly (0.1 + 0.2 ≠ 0.3).

## Consequences

- Reports can join budget and actual on the four key columns.
- The two entity classes share some duplicated fields. That is accepted for clarity.
- Supporting a non-January fiscal year later would require mapping fiscal month to calendar month.
