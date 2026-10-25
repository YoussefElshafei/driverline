# Architecture

> Status: MVP complete (Phases 0–6). Everything described here is built, tested, and runs with `docker compose up --build`. The optional Azure deployment and Excel add-in phases are not built.

## Services

| Component | Status | Responsibility |
| --- | --- | --- |
| `frontend` | Variance, forecast, budget entry and actuals import pages | React + TypeScript UI, served by nginx in Docker |
| `planning-service` | Reference data, budgets, actuals import, variance reports, forecasting and Excel export built | Accounts, departments, budgets, actuals, variances, forecasts, scenarios, Excel import/export |
| `commentary-service` | Built (port 8082, no database) | Writes plain-English commentary on variances it is given, through a swappable `LanguageModel` (Ollama) with an output guard and a rule-based fallback |
| PostgreSQL | Running via Docker Compose | Stores all planning data; schema managed by Flyway |
| Ollama | Runs natively on the host (port 11434) | Local language model server; `llama3.1:8b` by default. Optional: without it, commentary is rule-based |

```mermaid
flowchart LR
    frontend[frontend] -->|HTTP/JSON| planning[planning-service]
    planning -->|JDBC| db[(PostgreSQL)]
    planning -->|HTTP/JSON: totals + top 5 variances| commentary[commentary-service]
    commentary -->|HTTP: /api/chat| ollama[Ollama]
```

## Data model

```mermaid
erDiagram
    ACCOUNT ||--o{ BUDGET_LINE : "is budgeted in"
    ACCOUNT ||--o{ ACTUAL_LINE : "is recorded in"
    DEPARTMENT ||--o{ BUDGET_LINE : owns
    DEPARTMENT ||--o{ ACTUAL_LINE : owns
    ACCOUNT ||--o{ FORECAST_LINE : "is forecast by"
    DEPARTMENT ||--o{ FORECAST_LINE : owns
    FORECAST_LINE ||--o{ FORECAST_DRIVER_VALUE : "has values per"
    SCENARIO ||--o{ FORECAST_DRIVER_VALUE : "applies to"

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
    SCENARIO {
        bigint id PK
        varchar code UK "BASE, BEST, WORST"
        varchar name
        int sort_order
    }
    FORECAST_LINE {
        bigint id PK
        int fiscal_year
        bigint account_id FK
        bigint department_id FK
        varchar method "UNITS_X_RATE, PERCENT_OF_REVENUE, BUDGET"
        varchar quantity_label
        varchar rate_label
    }
    FORECAST_DRIVER_VALUE {
        bigint id PK
        bigint forecast_line_id FK
        bigint scenario_id FK
        numeric quantity "NUMERIC(12,2)"
        numeric rate "NUMERIC(15,2)"
        numeric percent "NUMERIC(7,4)"
    }
```

Forecast tables (Phase 4, [ADR 0013](adr/0013-driver-based-forecast-model.md), [ADR 0014](adr/0014-scenarios-as-driver-value-sets.md)): one `forecast_line` per account × department × year holds the method; `forecast_driver_value` holds the values for each scenario. Lines without a `forecast_line` are forecast at their budget.

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
├── forecast     Scenario, ForecastLine, ForecastDriverValue entities; ForecastService, controller, validator
│   ├── engine     ForecastInputAssembler and ForecastEngine (pure: actuals + drivers -> 12 months per line)
│   ├── dto        Forecast report, scenario comparison and driver grid shapes
│   └── export     Forecast workbook writer (monthly columns + scenario sheet)
├── commentary   GET /api/variance/commentary: picks the top variances, calls commentary-service (client, selector, service)
├── period       FiscalPeriod value object (year + month)
├── common       LineKey, MoneyParser, money-as-string JSON config, global error handler, OpenAPI config, shared Excel helpers
└── seed         Demo company profile, data generator, loader
```

`commentary-service` is small and has no database:

```
com.driverline.commentary
├── api          POST /api/commentary controller and request/response records
├── generation   CommentaryService (LLM first, fallback on failure), PromptBuilder, OutputGuard, RuleBasedCommentaryWriter, Amounts
├── llm          LanguageModel interface, OllamaLanguageModel, configuration (provider, URL, model, timeouts)
└── common       Problem-details error handler, OpenAPI config
```

The frontend is organised the same way:

```
frontend/src
├── api           Typed fetch client and API types
├── lib           money.ts: parsing, cents arithmetic, formatting
└── features
    ├── variance  Variance page: department (drill-down), account and monthly views, summary cards, AI commentary panel
    ├── forecast  Forecast page: scenario cards, 12-month table, driver editor
    ├── budgets   Budget entry page, grid, edit-tracking logic
    └── actuals   Import page and error table
```

## Demo data

On startup, if the database is empty, `planning-service` loads **Ace Janitorial Service**, a fictional commercial janitorial company with about 50 employees (see [ADR 0006](adr/0006-deterministic-demo-data-generator.md)):

- 4 departments, 12 accounts, 15 budgeted account/department lines
- FY2025: full budget and 12 months of actuals
- FY2026: full budget and actuals for January–September

Budgets follow seasonal patterns (e.g. specialty cleaning peaks in spring and August). Actuals add small random noise from a fixed seed, plus a few business events that produce visible variances, such as a 2026 fuel price spike and a contract lost in July 2026. It also loads FY2026 forecast drivers (Base/Best/Worst) for nine lines if the year has none yet, so databases seeded before Phase 4 receive them too. Set `SEED_DEMO_DATA=false` to start with an empty database.

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

### Forecast

```mermaid
flowchart LR
    req[GET /api/forecast?year&scenario] --> svc[ForecastService]
    svc --> facts[PlanningFactLoader<br/>budgets + actuals]
    svc --> cfg[ForecastLineRepository<br/>methods + driver values]
    facts --> asm[ForecastInputAssembler<br/>one input per line, scenario's drivers]
    cfg --> asm
    asm --> eng[ForecastEngine<br/>closed months: actuals<br/>open months: units x rate, budget,<br/>then % of revenue]
    eng --> rep[ForecastReportBuilder<br/>outlook vs budget, VarianceCalculator]
    rep --> json[JSON report]
```

Forecasts are calculated on every request from the stored drivers, so a driver edit or a new month of actuals shows up immediately. `GET /api/forecast/compare` runs the engine once per scenario.

### AI commentary

```mermaid
sequenceDiagram
    actor User
    participant UI as CommentaryPanel (React)
    participant P as planning-service
    participant C as commentary-service
    participant O as Ollama (llama3.1:8b)

    User->>UI: Generate commentary
    UI->>P: GET /api/variance/commentary?year&fromMonth&toMonth
    P->>P: Variance report + top 5 variances by dollars
    P->>C: POST /api/commentary {periodLabel, totals, highlights}
    C->>C: PromptBuilder (figures pre-formatted, direction in words)
    C->>O: POST /api/chat (temperature 0.2)
    alt answer arrives and passes OutputGuard
        O-->>C: text
        C-->>P: {text, provider: "ollama", model}
    else off, unreachable, timeout, error, or invented figures
        C->>C: RuleBasedCommentaryWriter
        C-->>P: {text, provider: "rule-based", fallbackReason}
    end
    P-->>UI: commentary + period
    Note over P,C: If commentary-service is down, planning-service returns 503
```

See [ADR 0015](adr/0015-llm-behind-an-interface-with-fallback-and-guard.md) and [ADR 0016](adr/0016-planning-orchestrates-stateless-commentary-service.md).

## Deployment (Docker Compose)

`docker compose up --build` runs the whole stack ([ADR 0017](adr/0017-docker-compose-nginx-and-host-ollama.md)):

```mermaid
flowchart LR
    browser[Browser] -->|localhost:8088| nginx
    subgraph compose[Docker Compose network]
        nginx[frontend<br/>nginx: React build + /api proxy]
        planning[planning-service :8080]
        commentary[commentary-service :8082]
        postgres[(postgres :5432)]
        nginx -->|/api, /swagger-ui, /v3/api-docs| planning
        planning -->|jdbc:postgresql://postgres:5432| postgres
        planning -->|http://commentary-service:8082| commentary
    end
    commentary -->|host.docker.internal:11434| ollama[Ollama on the host<br/>GPU, optional]
    devtools[Local development tools] -->|localhost:5434| postgres
```

| Container | Image | Published port | Health check | Starts after |
| --- | --- | --- | --- | --- |
| postgres | `postgres:17-alpine` | 5434 (for local development) | `pg_isready` | — |
| planning-service | built from `services/planning-service` (JRE 21, non-root) | none | `GET /actuator/health` (includes the database) | postgres healthy |
| commentary-service | built from `services/commentary-service` (JRE 21, non-root) | none | `GET /actuator/health` | — |
| frontend | built from `frontend` (Node build, nginx runtime) | **8088** | `GET /` | both services healthy |

Inside the network, services address each other by name (`postgres`, `commentary-service`), so compose sets `DB_URL` and `COMMENTARY_SERVICE_URL` explicitly rather than using the local-development values in `.env`. The database volume (`postgres-data`) persists between runs; `docker compose down -v` deletes it and the demo company is reseeded on the next start.

CI starts this same stack on every push and runs [`scripts/smoke-test.sh`](../scripts/smoke-test.sh) through nginx.

## Health checks

- `planning-service`: `GET /actuator/health` reports `UP` only when the service can reach the database.
- `commentary-service`: `GET /actuator/health` reports `UP` when the service is running. It deliberately doesn't depend on Ollama, because the service still works (rule-based) without it.
- `frontend`: nginx answers `GET /`.
