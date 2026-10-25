# Architecture

> Status: Phase 3. Data model, demo data, budget entry, actuals import (CSV/XLSX), variance reports with drill-down, and Excel export exist. Forecasting and AI commentary are not built yet.

## Services

| Component | Status | Responsibility |
| --- | --- | --- |
| `frontend` | Variance reports, budget entry and actuals import pages | React + TypeScript UI, served by nginx in Docker |
| `planning-service` | Reference data, budgets, actuals import, variance reports and Excel export built | Accounts, departments, budgets, actuals, variances, forecasts, scenarios, Excel import/export |
| `commentary-service` | Planned (Phase 5) | Generates plain-English variance commentary through a swappable LLM interface with a rule-based fallback |
| PostgreSQL | Running via Docker Compose | Stores all planning data; schema managed by Flyway |

```mermaid
flowchart LR
    frontend[frontend] -->|HTTP/JSON| planning[planning-service]
    planning -->|JDBC| db[(PostgreSQL)]
    planning -.->|HTTP/JSON, planned| commentary[commentary-service]
```

## Data model

```mermaid
erDiagram
    ACCOUNT ||--o{ BUDGET_LINE : "is budgeted in"
    ACCOUNT ||--o{ ACTUAL_LINE : "is recorded in"
    DEPARTMENT ||--o{ BUDGET_LINE : owns
    DEPARTMENT ||--o{ ACTUAL_LINE : owns

    ACCOUNT {
        bigint id PK
        varchar code UK "e.g. 4000"
        varchar name
        varchar type "REVENUE or EXPENSE"
    }
    DEPARTMENT {
        bigint id PK
        varchar code UK "e.g. OPS"
        varchar name
    }
    BUDGET_LINE {
        bigint id PK
        bigint account_id FK
        bigint department_id FK
        int fiscal_year
        int fiscal_month "1-12"
        numeric amount "NUMERIC(15,2)"
    }
    ACTUAL_LINE {
        bigint id PK
        bigint account_id FK
        bigint department_id FK
        int fiscal_year
        int fiscal_month "1-12"
        numeric amount "NUMERIC(15,2)"
    }
```

Key rules (see [ADR 0005](adr/0005-monthly-budget-and-actual-data-model.md)):

- One budget amount and one actual amount per **account × department × month**. A unique constraint on each table enforces this.
- The fiscal year matches the calendar year, so `fiscal_month = 1` is January.
- Amounts keep their natural sign (revenue and expenses are both positive). The account `type` decides whether a variance is favorable.
- Money is `NUMERIC(15,2)` in the database and `BigDecimal` in Java. It is never stored as floating point.
- The database enforces the same rules as the Java code (month 1–12, account type, uniqueness), so bad data can't get in through any path.

## Code layout

`planning-service` is organised by feature (see [ADR 0004](adr/0004-package-by-feature.md)):

```
com.driverline.planning
├── account      Account entity, AccountType, AccountRepository
├── department   Department entity, DepartmentRepository
├── reference    GET /api/accounts, /api/departments; ReferenceData lookup by code
├── budget       BudgetLine entity + repository, budget grid and save (controller, service, assembler, DTOs)
├── actual       ActualLine entity + repository
│   └── importing  CSV/XLSX parsers, row validator, import service and controller
├── variance     Variance rules (VarianceCalculator), reports and controller
│   ├── report     PlanningFact loading, ReportPeriod, VarianceReportBuilder (pure aggregation)
│   ├── dto        Report shapes returned by the API
│   └── export     ExcelReport layout, mapper and POI workbook writer
├── period       FiscalPeriod value object (year + month)
├── common       LineKey, MoneyParser, money-as-string JSON config, global error handler, OpenAPI config
└── seed         Demo company profile, data generator, loader
```

The frontend is organised the same way:

```
frontend/src
├── api           Typed fetch client and API types
├── lib           money.ts: parsing, cents arithmetic, formatting
└── features
    ├── variance  Variance page: department (drill-down), account and monthly views, summary cards
    ├── budgets   Budget entry page, grid, edit-tracking logic
    └── actuals   Import page and error table
```

## Demo data

On startup, if the database is empty, `planning-service` loads **Ace Janitorial Service**, a fictional commercial janitorial company with about 50 employees (see [ADR 0006](adr/0006-deterministic-demo-data-generator.md)):

- 4 departments, 12 accounts, 15 budgeted account/department lines
- FY2025: full budget and 12 months of actuals
- FY2026: full budget and actuals for January–September

Budgets follow seasonal patterns (e.g. specialty cleaning peaks in spring and August). Actuals add small random noise from a fixed seed, plus a few business events that produce visible variances, such as a 2026 fuel price spike and a contract lost in July 2026. Set `SEED_DEMO_DATA=false` to start with an empty database.

## Request flow

In development the browser only talks to the Vite dev server (port 5173), which forwards `/api/*` to planning-service (port 8080). Same origin for the browser means no CORS setup.

### Saving a budget

```mermaid
sequenceDiagram
    actor User
    participant UI as BudgetEntryPage (React)
    participant C as BudgetController
    participant S as BudgetService
    participant R as BudgetLineRepository
    participant DB as PostgreSQL

    User->>UI: Edits cells, clicks Save
    UI->>UI: Validate inputs, keep only changed cells (lib/money.ts, budgetEdits.ts)
    UI->>C: PUT /api/budgets {year, entries}
    C->>C: Bean Validation (@Valid): month 1-12, amount <= 2 decimals
    C->>S: save(request)
    S->>S: Check codes exist, no duplicate cells
    S->>R: findByPeriodYear(year)
    R->>DB: SELECT ... JOIN account, department
    S->>S: Update existing lines / create new ones
    S->>R: saveAll(new lines)
    Note over S,DB: One transaction: commit writes updates + inserts
    S-->>C: {created, updated}
    C-->>UI: 200 JSON
    UI->>C: GET /api/budgets?year=... (reload grid)
```

Any validation failure becomes an RFC 9457 problem-details response from `GlobalExceptionHandler` (400), and nothing is saved.

### Importing actuals

```mermaid
flowchart LR
    file[.csv / .xlsx upload] --> ctrl[ActualsImportController]
    ctrl --> svc[ActualsImportService]
    svc --> pick{Parser for file type}
    pick -->|.csv| csv[CsvActualsParser<br/>Commons CSV]
    pick -->|.xlsx| xlsx[XlsxActualsParser<br/>Apache POI]
    pick -->|other| e415[415 Unsupported]
    csv --> raw[RawActualRow list<br/>all values as text]
    xlsx --> raw
    raw --> val[ActualRowValidator<br/>collects every error]
    val -->|any errors| e422[422 with row errors<br/>nothing saved]
    val -->|all valid| upsert[Update existing / create new<br/>one transaction]
    upsert --> ok[200 ImportSummary]
```

### Variance report

```mermaid
flowchart LR
    req[GET /api/variance/by-department] --> svc[VarianceService]
    svc --> load[PlanningFactLoader<br/>2 queries: budgets + actuals for the year]
    load --> facts[PlanningFact list<br/>one per account x department x month]
    facts --> period[ReportPeriod.resolve<br/>default Jan to latest closed month]
    period --> build[VarianceReportBuilder<br/>filter, group, net, sum]
    build --> calc[VarianceCalculator<br/>variance, %, favorability]
    calc --> json[JSON report]
    json -.export.-> excel[VarianceExcelMapper -> VarianceWorkbookWriter<br/>.xlsx with formulas + grouping]
```

Department totals are **net contribution** (revenue − expenses), so departments add up to company net income and each department's accounts add up to its total ([ADR 0011](adr/0011-net-contribution-and-closed-months.md)).

## Health checks

`planning-service` exposes `GET /actuator/health`. It reports `UP` only when the service can reach the database.
