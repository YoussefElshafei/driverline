# planning-service API

Interactive documentation is generated from the code by springdoc. With the service running:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI spec (JSON): <http://localhost:8080/v3/api-docs>

This page summarises the endpoints and the conventions that apply to all of them.

## Conventions

- **Money** is always a JSON string with 2 decimals, e.g. `"1234.50"` ([ADR 0009](adr/0009-money-as-strings-in-json.md)). Requests may send a string or a number.
- **Errors** use RFC 9457 problem details (`application/problem+json`), with an `errors` array when specific fields or file rows are at fault ([ADR 0007](adr/0007-problem-details-error-responses.md)).
- **Months** are 1–12; the fiscal year is the calendar year.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/accounts` | Chart of accounts, ordered by code |
| GET | `/api/departments` | Departments, ordered by code |
| GET | `/api/budgets?year=2026[&department=OPS]` | Budget grid: one row per account and department with 12 monthly amounts (`null` where not entered) and a total |
| PUT | `/api/budgets` | Create or update budget cells. All-or-nothing |
| POST | `/api/actuals/import` | Import actuals from a `.csv` or `.xlsx` file (multipart field `file`). All-or-nothing; re-imported cells are replaced ([ADR 0008](adr/0008-all-or-nothing-actuals-import-with-upsert.md)) |
| GET | `/api/actuals/import/template` | Example CSV with the required columns |
| GET | `/api/variance/by-department?year=2026[&fromMonth=1&toMonth=9]` | Company summary plus each department's net contribution, with its accounts nested (drill-down) |
| GET | `/api/variance/by-account?year=2026[&fromMonth&toMonth]` | P&L layout: revenue and expense accounts combined across departments |
| GET | `/api/variance/by-month?year=2026[&department=FLEET][&account=5300]` | 12-month trend for net income, a department's net contribution, or one account |
| GET | `/api/variance/{by-department,by-account,by-month}/export?...` | The same report as a formatted `.xlsx` workbook |
| GET | `/api/forecast?year=2026[&scenario=BASE]` | Full-year outlook for a scenario: 12 months per line, each marked actual or forecast, grouped by department |
| GET | `/api/forecast/compare?year=2026` | Base, Best and Worst outlooks side by side |
| GET | `/api/forecast/scenarios` | The scenarios |
| GET | `/api/forecast/drivers?year=2026` | Every forecastable line with its method, labels and values per scenario |
| PUT | `/api/forecast/drivers` | Create or update methods and driver values. All-or-nothing |
| GET | `/api/forecast/export?year=2026[&scenario=BASE]` | Forecast workbook (monthly columns, forecast months shaded) plus a scenario comparison sheet |
| GET | `/actuator/health` | `UP` when the service can reach the database |

### Variance reports

Rules ([ADR 0010](adr/0010-variance-sign-and-percentage-rules.md), [ADR 0011](adr/0011-net-contribution-and-closed-months.md)):

- `variance = actual − budget`; `variancePercent = variance ÷ |budget| × 100` (1 decimal), `null` when the budget is 0.
- `favorability`: `FAVORABLE`, `UNFAVORABLE` or `ON_BUDGET`. Revenue and net figures: higher is better. Expenses: lower is better.
- Department totals are **net contribution** (revenue − expenses).
- `fromMonth`/`toMonth` default to January through `latestActualMonth` (the last month with any actuals). Within the range, a missing actual counts as zero.

Every report line has this shape (real demo data: Vehicle & Fuel, FY2026 January–September):

```json
{ "budget": "142065.00", "actual": "156518.61", "variance": "14453.61", "variancePercent": "10.2", "favorability": "UNFAVORABLE" }
```

`GET /api/variance/by-department?year=2026` (abridged):

```json
{
  "year": 2026, "fromMonth": 1, "toMonth": 9, "latestActualMonth": 9,
  "summary": { "revenue": { ... }, "expenses": { ... }, "netIncome": { ... } },
  "departments": [
    {
      "departmentCode": "FLEET", "departmentName": "Fleet & Equipment",
      "netContribution": { ... },
      "accounts": [ { "accountCode": "5300", "accountName": "Vehicle & Fuel", "accountType": "EXPENSE", "line": { ... } } ]
    }
  ]
}
```

In `by-month`, each of the 12 months has `budget`, `closed` and `variance` (`null` for months without actuals), plus `closedMonthsTotal`.

**Excel exports** ([ADR 0012](adr/0012-excel-export-with-live-formulas.md)) return `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` with a descriptive file name such as `variance-by-department-FY2026-Jan-Sep.xlsx`. Variance columns are live Excel formulas; account rows are grouped under their department.

### Forecast

Rules ([ADR 0013](adr/0013-driver-based-forecast-model.md), [ADR 0014](adr/0014-scenarios-as-driver-value-sets.md)):

- Full-year outlook = actuals for closed months + forecast for open months. Each month in `months` has `amount` and `actual` (`true` = recorded actual, `false` = forecast).
- Methods: `UNITS_X_RATE` (quantity × rate), `PERCENT_OF_REVENUE` (percent of that month's total forecast revenue; expense lines only), `BUDGET` (the month's budget; the default).
- `outlook` lines compare the full-year budget with the outlook using the variance rules: `{ "budget", "outlook", "variance", "variancePercent", "favorability" }`.

Save drivers (values are required for every scenario unless the method is `BUDGET`):

```http
PUT /api/forecast/drivers
Content-Type: application/json

{
  "year": 2026,
  "lines": [{
    "accountCode": "5300", "departmentCode": "FLEET", "method": "UNITS_X_RATE",
    "quantityLabel": "Vans", "rateLabel": "Monthly cost per van",
    "values": {
      "BASE":  { "quantity": "14", "rate": "1250.00" },
      "BEST":  { "quantity": "14", "rate": "1150.00" },
      "WORST": { "quantity": "14", "rate": "1350.00" }
    }
  }]
}
```

Errors point at the exact input, e.g. `lines[0].values.BEST.rate: is required for units × rate` or `lines[0].method: A revenue line can't be a percentage of revenue`.

### Save budget

```http
PUT /api/budgets
Content-Type: application/json

{
  "year": 2026,
  "entries": [
    { "accountCode": "6100", "departmentCode": "SALES", "month": 3, "amount": "9000.00" }
  ]
}
```

Response: `{ "year": 2026, "created": 0, "updated": 1 }`

Validation error example (400):

```json
{
  "title": "Validation failed",
  "status": 400,
  "detail": "Some budget entries are invalid. Nothing was saved.",
  "instance": "/api/budgets",
  "errors": [{ "field": "entries[0].accountCode", "message": "Unknown account code '9999'" }]
}
```

### Import actuals

File columns (header row required, any order, any case): `year, month, account_code, department_code, amount`.

Success (200):

```json
{ "fileName": "actuals-2026-10.csv", "rowsImported": 15, "created": 15, "updated": 0, "periods": ["2026-10"] }
```

Rejected file (422). `row` matches the row number shown in Excel; `row: null` means a whole-file problem:

```json
{
  "title": "Import failed",
  "status": 422,
  "detail": "Found 2 problems in the file. No rows were imported.",
  "errors": [
    { "row": 3, "column": "month", "value": "13", "message": "must be between 1 and 12" },
    { "row": 5, "column": "amount", "value": "26276.545", "message": "'26276.545' has more than 2 decimal places" }
  ]
}
```

Other statuses: 415 for unsupported file types, 413 for files over 5 MB, 400 when the `file` part is missing.
