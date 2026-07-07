# URL Shortener + Agentic SDLC Orchestration

A URL shortener backend (Java 21 / Spring Boot) plus a real, runnable orchestration
engine (Python, standard library only) that governs how changes are made to it —
built for the "Agentic Software Engineering System" assignment.

## Start here

- [`docs/SETUP.md`](./docs/SETUP.md) — run the backend and the orchestrator.
- [`docs/ORCHESTRATION_ARCHITECTURE.md`](./docs/ORCHESTRATION_ARCHITECTURE.md) — what
  the orchestration layer is and how it works.
- [`docs/SCENARIOS.md`](./docs/SCENARIOS.md) — the three required scenarios
  (greenfield/brownfield/ambiguous), each driven through the orchestrator against
  this repo's real codebase.
- [`docs/TESTING_AND_LIMITATIONS.md`](./docs/TESTING_AND_LIMITATIONS.md) — testing
  approach, known limitations, and deliberate trade-offs.
- [`docs/FINAL_SUMMARY.md`](./docs/FINAL_SUMMARY.md) — plan, rationale, artifacts,
  risks, assumptions.

## Backend docs

- [`docs/BACKEND.md`](./docs/BACKEND.md) — features and how they work.
- [`docs/schemas.md`](./docs/schemas.md) — database and JSON schemas.
- [`docs/FRONTEND_INTEGRATION.md`](./docs/FRONTEND_INTEGRATION.md) — building a
  frontend against this API.
- [`openapi.yml`](./openapi.yml) — formal API contract.
