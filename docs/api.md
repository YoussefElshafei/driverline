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
| GET | `/actuator/health` | `UP` when the service can reach the database |

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
