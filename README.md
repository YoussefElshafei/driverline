# DriverLine

[![CI](https://github.com/YoussefElshafei/driverline/actions/workflows/ci.yml/badge.svg)](https://github.com/YoussefElshafei/driverline/actions/workflows/ci.yml)

A budget vs. actual variance analysis and driver-based forecasting app for a small company's finance team.

> **Status:** in development (Phase 5 of 6). All planned features work today: budget entry, actuals import, variance reports with drill-down, Excel export, driver-based forecasting with scenarios, and AI variance commentary. Phase 6 (running the whole stack in Docker, screenshots, final documentation) is next.

## Why this exists

Financial planning and analysis (FP&A) teams set a budget for the year, compare it every month against what the company actually spent and earned, and explain the differences (variances) to leadership. They also re-forecast the rest of the year as conditions change. Much of this work is done in spreadsheets. DriverLine is a portfolio project that models that workflow in a web app built on a Java/Spring Boot and React/TypeScript stack.

All data in this project is for a fictional company.

## Demo company

On first start, the backend loads **Ace Janitorial Service**, a fictional commercial janitorial company with about 50 employees and about $4M in annual revenue:

- 4 departments (Sales & Account Management, Operations, Fleet & Equipment, Administration) and 12 accounts
- FY2025 budget and a full year of actuals; FY2026 budget and actuals for January–September
- Seasonal patterns (spring and back-to-school peaks for specialty cleaning, winter peaks for fuel and utilities) and a few business events that create visible variances

The data is generated in code from a fixed random seed, so every run produces the same numbers. Set `SEED_DEMO_DATA=false` to start empty. See [docs/architecture.md](docs/architecture.md#demo-data).

## Features

**Working now**

- **Chart of accounts and departments**, seeded for the demo company and available through the API
- **Monthly budget entry:** an editable grid per fiscal year and department, with live row totals, inline validation, adding account rows, and all-or-nothing saves
- **Actuals import from CSV and Excel (.xlsx):** every row is validated before anything is saved; a rejected file returns every problem with its row number, column and value. Re-importing a month replaces its amounts instead of duplicating them. Accepts `1,234.56`, `$1,234.56` and accounting-style `(1,234.56)`
- **Budget vs. actual variance reports** by department, by account and by month, with dollar and percent variances and favorable/unfavorable flags that respect account type (revenue above budget is favorable; expenses above budget are unfavorable)
- **Drill-down** from department totals (net contribution) to individual accounts, and from any account to its monthly trend
- **Period selection** that defaults to the months with actuals and warns when a range goes past them
- **Excel export** of every report: live variance formulas, expandable department groups, accounting formats, colour-coded status, print-ready layout
- **Driver-based forecasting:** the full-year outlook combines actuals for closed months with a forecast for the rest of the year, driven by business quantities: units × rate (e.g. active contracts × average contract value, cleaners × monthly cost), percent of revenue (e.g. supplies), or budget for fixed costs
- **Base / Best / Worst scenarios:** separate driver values per scenario, edited side by side with a live preview, compared on one screen and exported to Excel (monthly columns with forecast months shaded, `SUM` formulas, and a scenario comparison sheet)
- **AI variance commentary:** a plain-English explanation of the period's five largest variances, written by a local language model (Ollama, `llama3.1:8b` by default) in a separate commentary-service. Every figure of 1,000 or more in the model's answer is checked against the data before it is shown; if the model is off, unreachable, slow, or its answer fails the check, a rule-based summary is returned instead, labelled with the reason
- **API documentation** generated with springdoc (Swagger UI)

## Architecture

```mermaid
flowchart LR
    user[Finance user] --> frontend[React frontend]
    frontend --> planning[planning-service<br/>Spring Boot]
    planning --> db[(PostgreSQL)]
    planning -->|largest variances| commentary[commentary-service<br/>Spring Boot]
    commentary -->|LanguageModel interface| llm[Ollama<br/>local LLM]
    commentary -.->|if unavailable| rules[Rule-based writer]
```

See [docs/architecture.md](docs/architecture.md) for details and [docs/adr/](docs/adr/) for the decisions behind the design.

## Tech stack

| Area | Technology | Why |
| --- | --- | --- |
| Frontend | React + TypeScript (Vite) | Component-based UI with static types; Vite gives fast builds and dev server |
| Frontend tests | Vitest + React Testing Library | Vite-native test runner; tests components the way a user interacts with them |
| Frontend quality | ESLint + Prettier | Catch bugs and keep formatting consistent automatically |
| Backend | Java 21 + Spring Boot 4, Maven | Mature, widely used stack for business services |
| Database | PostgreSQL + Flyway | Reliable relational database; versioned, repeatable schema migrations |
| Backend tests | JUnit 5, Mockito, Testcontainers | Unit tests plus integration tests against a real PostgreSQL in Docker |
| File import | Apache POI, Apache Commons CSV | POI reads .xlsx workbooks; Commons CSV handles quoting and edge cases a hand-written parser gets wrong |
| API docs | springdoc-openapi | Generates OpenAPI/Swagger UI from the controllers, so docs can't drift from the code |
| Routing | React Router | Standard client-side routing for the frontend pages |
| AI commentary | Ollama (local LLM, `llama3.1:8b`) behind a `LanguageModel` interface | Free, private and runs on a laptop GPU; the interface lets the provider be swapped, and a rule-based fallback keeps the app working without it |
| Containers | Docker + Docker Compose | Same environment on every machine |
| CI | GitHub Actions | Builds, tests and lints on every push and pull request |

## Running the project

### Prerequisites

- Docker Desktop
- JDK 21 or newer
- Node.js 24
- Optional: [Ollama](https://ollama.com) for AI commentary (without it, commentary is rule-based)

### Start the database

```powershell
Copy-Item .env.example .env
docker compose up -d
```

### Run the backend

```powershell
cd services\planning-service
.\mvnw.cmd spring-boot:run
```

Health check: <http://localhost:8080/actuator/health>
API docs: <http://localhost:8080/swagger-ui.html> (summary in [docs/api.md](docs/api.md))

On startup, Flyway creates the schema and the demo company is loaded into the empty database. To reset the database, run `docker compose down -v` (this deletes the data volume), then `docker compose up -d`.

### Run the commentary service (for AI commentary)

In a second terminal:

```powershell
ollama pull llama3.1:8b
cd services\commentary-service
.\mvnw.cmd spring-boot:run
```

It listens on port 8082 and uses Ollama at `http://localhost:11434`. The model download (about 4.9 GB) is needed once. Without Ollama, the service still answers with rule-based commentary; set `COMMENTARY_LLM_PROVIDER=none` to skip the language model entirely. `llama3.1:8b` fits in 8 GB of GPU memory; on a CPU it works but takes much longer.

### Run the frontend

```powershell
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>. The dev server forwards `/api` requests to the backend on port 8080.

### Try it

1. **Variance:** the home page opens on FY2026 (January–September). Expand **Fleet & Equipment** to see the fuel price spike on Vehicle & Fuel, click **Trend** to see it month by month, use **Export to Excel**, and click **Generate commentary** for a plain-English summary of the largest variances.
2. **Forecast:** compare the Base, Best and Worst full-year net income, click a scenario to see its months (October–December shaded as forecast), then change a driver (e.g. active contracts) and save to see the outlook update.
3. **Budget entry:** pick FY2026 and a department, change a few cells (changed cells turn yellow, invalid ones red), and save.
4. **Import actuals:** upload [`samples/actuals-2026-10.csv`](samples/) or the `.xlsx` version to add October 2026, then upload `samples/actuals-with-errors.csv` to see row-level validation errors. After importing October, the variance page offers October in its range and the forecast treats October as actual.

## Running tests

Backend unit tests only (fast, no Docker needed):

```powershell
cd services\planning-service
.\mvnw.cmd test
```

Backend unit and integration tests (planning-service's integration tests need Docker running):

```powershell
cd services\planning-service
.\mvnw.cmd verify
cd ..\commentary-service
.\mvnw.cmd verify
```

commentary-service's tests never call a real language model: it is mocked, and its integration tests run with the model turned off or unreachable.

Frontend:

```powershell
cd frontend
npm run lint
npm run format:check
npm run typecheck
npm test
```

## Project structure

```
.
├── .github/workflows/   CI pipeline
├── docs/                Architecture notes and decision records
├── frontend/            React + TypeScript app
├── samples/             Example actuals files (valid CSV/XLSX and one with deliberate errors)
├── services/
│   ├── planning-service/    Spring Boot: planning data, reports, forecasts, Excel
│   └── commentary-service/  Spring Boot: AI variance commentary with rule-based fallback
├── docker-compose.yml   Local PostgreSQL
└── .env.example         Environment variable template
```
