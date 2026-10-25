# ADR 0016: planning-service orchestrates; commentary-service is stateless

**Status:** Accepted

## Context

The project has two backend services. Commentary needs variance data, which planning-service owns. Either the frontend asks commentary-service, which fetches data from planning-service, or the frontend asks planning-service, which sends the data to commentary-service.

## Decision

- The frontend calls only planning-service: `GET /api/variance/commentary?year&fromMonth&toMonth`.
- planning-service builds the variance report, picks the **five largest account variances by dollar amount** (`VarianceHighlightSelector`), and posts the totals and highlights to commentary-service's `POST /api/commentary`.
- **commentary-service has no database and no knowledge of planning-service.** It turns the figures it is given into text and never recalculates them.
- The services share no code (ADR 0001). The request and response records are duplicated, and the JSON contract is documented in `docs/api.md` and tested on both sides (planning's client test checks what it sends; commentary's controller test accepts that JSON).
- If commentary-service is down, planning-service returns **503** with a problem-details body; the rest of the app keeps working.

## Alternatives considered

- **commentary-service fetches from planning-service.** The frontend would talk to two backends (more proxy and CORS setup), and commentary-service would depend on planning-service's API and availability.
- **One service.** Simpler to run, but the language model integration has different dependencies and scaling needs (a GPU machine), which is the reason for a separate service.
- **A shared DTO library.** Removes duplication but couples the two services' release cycles.

## Consequences

- commentary-service can be tested, deployed and scaled on its own (for example, on the machine with the GPU), and could explain forecasts later with no changes.
- Numbers have one owner: planning-service calculates, commentary-service only describes.
- The contract is duplicated in two places, and documentation plus tests keep the copies aligned.
