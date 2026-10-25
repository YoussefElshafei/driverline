# ADR 0001: Monorepo with independently built services

**Status:** Accepted

## Context

The project has a React frontend and two backend services (`planning-service`, `commentary-service`). One person develops all of them, and they change together often.

## Decision

Keep everything in one Git repository. Each service is its own standalone Maven project with its own Maven wrapper, Dockerfile and CI matrix entry. There is no shared parent POM.

## Alternatives considered

- **One repository per service.** Closer to how large organisations split ownership, but adds cross-repo coordination with no benefit for a single developer.
- **Maven multi-module build with a parent POM.** Less duplication, but couples the services' builds and versions, which works against the point of having separate services.

## Consequences

- One clone and one CI pipeline cover the whole system.
- Each service can be built, tested and containerised on its own.
- Some build configuration is duplicated between services.
