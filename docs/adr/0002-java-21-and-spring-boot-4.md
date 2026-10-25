# ADR 0002: Target Java 21 LTS with Spring Boot 4

**Status:** Accepted

## Context

The backend must use a Java LTS release (21 or newer) and the latest stable Spring Boot. Java 21 and Java 25 are both LTS releases. Spring Boot 4.1.x supports both.

## Decision

Compile for Java 21 (`<java.version>21</java.version>`) and use Spring Boot 4.1.2 (the newest stable release reported by Maven Central when the project was set up). Docker images and CI use Eclipse Temurin 21.

## Alternatives considered

- **Java 25.** The newest LTS. It was not chosen because the development machine currently has JDK 24, which can compile Java 21 bytecode but not Java 25. Moving to 25 later only requires changing `java.version`, the CI `java-version` and the Docker base images.
- **Spring Boot 3.5.** More tutorials and answers online, but it is no longer the latest major version.

## Consequences

- Any JDK from 21 upward can build the project.
- Spring Boot 4 split auto-configuration into smaller modules, so some starters have new names (for example `spring-boot-starter-webmvc` and `spring-boot-starter-flyway`). Older tutorials may use the old names.
