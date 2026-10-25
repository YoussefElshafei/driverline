# DriverLine

[![CI](https://github.com/YoussefElshafei/driverline/actions/workflows/ci.yml/badge.svg)](https://github.com/YoussefElshafei/driverline/actions/workflows/ci.yml)

A budget vs. actual variance analysis and driver-based forecasting app for a small company's finance team.

> **Status:** in development (Phase 3 of 6). Budget entry, actuals import, variance reports with drill-down, and Excel export work today; driver-based forecasting and AI commentary are planned. This README is updated as each feature ships.

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
- **API documentation** generated with springdoc (Swagger UI)

**Planned**

- Driver-based forecasting with Base / Best / Worst scenarios
- Plain-English variance commentary from a local LLM, with a rule-based fallback

## Architecture

```mermaid
flowchart LR
    user[Finance user] --> frontend[React frontend]
    frontend --> planning[planning-service<br/>Spring Boot]
    planning --> db[(PostgreSQL)]
    planning -.planned.-> commentary[commentary-service<br/>Spring Boot]
    commentary -.planned.-> llm[Ollama / rule-based fallback]
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
| Containers | Docker + Docker Compose | Same environment on every machine |
| CI | GitHub Actions | Builds, tests and lints on every push and pull request |

## Running the project

### Prerequisites

- Docker Desktop
- JDK 21 or newer
- Node.js 24

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

### Run the frontend

```powershell
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>. The dev server forwards `/api` requests to the backend on port 8080.

### Try it

1. **Variance:** the home page opens on FY2026 (January–September). Expand **Fleet & Equipment** to see the fuel price spike on Vehicle & Fuel, click **Trend** to see it month by month, and use **Export to Excel**.
2. **Budget entry:** pick FY2026 and a department, change a few cells (changed cells turn yellow, invalid ones red), and save.
3. **Import actuals:** upload [`samples/actuals-2026-10.csv`](samples/) or the `.xlsx` version to add October 2026, then upload `samples/actuals-with-errors.csv` to see row-level validation errors. After importing October, the variance page offers October in its range.

## Running tests

Backend unit tests only (fast, no Docker needed):

```powershell
cd services\planning-service
.\mvnw.cmd test
```

Backend unit and integration tests (integration tests need Docker running):

```powershell
cd services\planning-service
.\mvnw.cmd verify
```

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
│   └── planning-service/  Spring Boot service
├── docker-compose.yml   Local PostgreSQL
└── .env.example         Environment variable template
```
