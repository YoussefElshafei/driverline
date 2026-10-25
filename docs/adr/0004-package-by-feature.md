# ADR 0004: Organise backend code by feature

**Status:** Accepted

## Context

The service will hold several business areas (accounts, departments, budgets, actuals, variances, forecasts, import/export). Each will have an entity, repository, service, controller and DTOs. Each class still belongs to one layer (controller → service → repository).

## Decision

Group code into one package per feature (`account`, `budget`, `actual`, ...). Inside each package, classes still follow the controller → service → repository layering.

## Alternatives considered

- **Package by layer** (`controller`, `service`, `repository`, `entity`). Common in tutorials, but one feature ends up spread across four or five packages, and every class must be public.

## Consequences

- Everything about one feature lives together, which makes it easier to find, change and delete.
- Some types are shared across features (e.g. `FiscalPeriod`) and live in their own small package.
