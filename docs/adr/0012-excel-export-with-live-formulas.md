# ADR 0012: Excel export with live formulas and row grouping

**Status:** Accepted

## Context

Finance teams live in Excel. An exported report should feel native: numbers people can audit and reuse, drill-down that works in Excel, and formatting that prints well.

## Decision

- Each report is first mapped to a neutral `ExcelReport` (title, subtitles, rows with a kind and outline level) by `VarianceExcelMapper`; `VarianceWorkbookWriter` renders any `ExcelReport` with Apache POI. Adding a report means adding a mapping, not another writer.
- **Budget and actual are values; variance and variance % are formulas:** `=ROUND(C5-B5,2)` and `=IF(B5=0,"n/a",D5/ABS(B5))`. Formula results are calculated and stored before saving, so viewers that don't recalculate still show numbers.
- **`ROUND(…, 2)`** keeps the stored variance at whole cents. Without it, Excel's binary floating-point subtraction left values like `-1251.2700000000186`, which would leak into any formula referencing the cell. (The integration test comparing the workbook to the API caught this.)
- **Account rows are grouped** (outline level 1) under their department row, with the +/- control on the department row, so users can expand and collapse departments in Excel.
- Accounting number format `#,##0.00;(#,##0.00)`, percentage format `0.0%`, green/rose status cells, bold totals with a top border, frozen header row and label column, landscape fit-to-width printing.

## Alternatives considered

- **All values, no formulas.** Exact and simple, but users can't see how the variance was calculated or reuse the logic.
- **CSV export.** Trivial, but loses formatting, grouping and formulas, which are the point of a finance report.
- **Excel templates filled with data.** Good for heavily designed reports, but adds a binary template to maintain.

## Consequences

- Exports are auditable and behave like hand-built Excel reports.
- Excel stores numbers as doubles; amounts with 2 decimals are well within its 15 significant digits, and the variance formula rounds to cents.
