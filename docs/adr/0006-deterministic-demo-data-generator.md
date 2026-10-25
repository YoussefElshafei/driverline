# ADR 0006: Generate demo data in Java with a fixed random seed

**Status:** Accepted

## Context

The app needs a realistic fictional company with seasonality and at least a full year of actuals, so anyone can explore reports immediately. The data must never come from a real company, and tests need to rely on it.

## Decision

- Describe the company in a `DemoCompanyProfile` record: departments, accounts, monthly base amounts, seasonality per line, growth per year, and "story events" that push actuals away from budget.
- `SeedDataGenerator` is a pure function from profile to dataset. Budgets are base × seasonality × growth. Actuals are budget × random noise × story events. Randomness uses `java.util.Random` with a fixed seed, and the noise is drawn as whole basis points so all arithmetic stays in `BigDecimal`.
- `DemoDataLoader` saves the dataset in one transaction, only if the database has no departments. `DemoDataSeeder` runs it at startup when `driverline.seed.demo-data` is true (env `SEED_DEMO_DATA`, default `true`).

## Alternatives considered

- **Static SQL inserts in a Flyway migration.** Simple, but hundreds of hand-written rows are hard to review and change, and seasonality and story events would be invisible in the SQL. It would also force demo data into every environment, including production.
- **Load from a CSV file.** Would exercise the Phase 2 import path, but the numbers would still need generating somehow, and their logic wouldn't be testable.

## Consequences

- The data is reproducible and unit-tested (exact formula values, seasonality, story events, determinism).
- Changing the demo company means editing one Java class.
- Seeding is skipped silently on a non-empty database. To reseed, recreate the database volume (`docker compose down -v`).
