# ADR 0017: One Docker Compose stack, nginx as the single entry point, Ollama on the host

**Status:** Accepted

## Context

Anyone cloning the repository should be able to run the whole app with one command. The stack has a database, two Spring Boot services, a React frontend, and an optional local language model that needs a GPU.

## Decision

- **`docker compose up --build` runs everything:** postgres, planning-service, commentary-service and frontend. Each has a health check (`pg_isready`, Spring Boot Actuator `/actuator/health` via `curl`, nginx `/`), and `depends_on` waits for health, so `docker compose up --wait` returns only when the app is usable.
- **nginx is the single entry point (port 8088):** it serves the built React app and proxies `/api` (and Swagger UI) to planning-service. The browser sees one origin, so there's no CORS configuration, matching the Vite proxy used in development. Only nginx (8088) and PostgreSQL (5434, for local development) are published; the Java services stay on the internal Docker network.
- **Ollama stays on the host,** not in Compose. commentary-service reaches it at `host.docker.internal:11434` (`extra_hosts: host-gateway` makes this work on Linux too). The URL used inside containers has its own variable (`OLLAMA_URL_FROM_DOCKER`), because the local-development value (`localhost:11434`) would point at the container itself.
- **Configuration through environment variables with defaults,** so no `.env` file is needed; `.env.example` documents them.
- **CI runs the real stack:** it builds all images, starts Compose, waits for health, runs `scripts/smoke-test.sh` through nginx (commentary falls back to rule-based because there's no Ollama on the runner), prints logs on failure, and tears down.

## Alternatives considered

- **Ollama in Compose with GPU passthrough.** One command would include the model, but GPU passthrough on Windows (WSL2) is fiddly, the 4.9 GB model would live in a Docker volume, and CI has no GPU anyway.
- **Expose every service port.** Convenient for debugging, but clashes with services run locally on 8080 and 8082 and widens the surface; Swagger is reachable through nginx instead.
- **Serve the frontend from planning-service.** One fewer container, but couples the frontend build to the Java build and loses nginx's static file serving.

## Consequences

- The quality goal "clone and run with one command" is tested on every push.
- AI commentary in Docker depends on the host running Ollama; without it the app still works with rule-based commentary.
- The database volume persists between runs; `docker compose down -v` resets the demo data.
