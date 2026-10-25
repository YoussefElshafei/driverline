# ADR 0003: Separate unit tests from Docker-backed integration tests

**Status:** Accepted

## Context

Integration tests use Testcontainers to start a real PostgreSQL in Docker. They are slower and need Docker. Unit tests for calculation logic (variances, forecasts) should run in seconds with no external dependencies.

## Decision

- Unit tests are named `*Test` and run with Maven Surefire during `mvn test`.
- Integration tests are named `*IT` and run with Maven Failsafe during `mvn verify`.
- CI always runs `mvn verify`, so both run on every push.

## Alternatives considered

- **Run everything with Surefire.** Simpler, but every local test run would need Docker and would be slower.
- **H2 in-memory database for integration tests.** Faster, but H2 behaves differently from PostgreSQL, so tests could pass while production fails.

## Consequences

- `mvn test` gives fast feedback without Docker.
- Integration tests run against the same database engine and version used in production.
