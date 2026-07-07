# URL Shortener + Agentic SDLC Orchestration

A production-quality URL shortener backend (Java 21 / Spring Boot) plus a real,
runnable **agentic orchestration engine** (Python, standard library only) that governs
how changes are made to it — requirements → design → implementation → test → docs →
compliance → release, with dependency graphs, gates, bounded retry/rollback, human
approval checkpoints, audit logging, and reliability metrics.

Built for the "Agentic Software Engineering System" interview assignment. See
[`SUBMISSION.md`](./SUBMISSION.md) (or `SUBMISSION.pdf`) for the full write-up of what
was built, why, and how it satisfies each part of the assignment.

## What's in this repo

| Part | What it is | Where |
|---|---|---|
| Backend | URL shortener API: shorten/redirect/expiry/click-count, custom aliases, retention policy, strict URL validation, CORS, QR codes, analytics | `src/`, `pom.xml` |
| Orchestration engine | DAG scheduler with gates/retry/rollback/approval/replanning, driving three real scenarios against this backend | `orchestrator/` |
| Documentation | Architecture, scenarios, setup, testing/limitations, API flows, final summary | `docs/` |

## Quick start

**Backend:**
```bash
docker compose up -d          # local Postgres
./mvnw spring-boot:run        # mvnw.cmd on Windows
./mvnw test                   # run the test suite
```

**Orchestrator:**
```bash
python orchestrator/run.py start --pipeline orchestrator/pipelines/greenfield.json --run-id my-run
python -m unittest discover -s orchestrator/tests -t .   # the engine's own test suite
```

Full instructions: [`docs/SETUP.md`](./docs/SETUP.md).

## Documentation index

**Orchestration & assignment deliverables**
- [`SUBMISSION.md`](./SUBMISSION.md) — the full submission write-up (also available as `SUBMISSION.pdf`).
- [`docs/ORCHESTRATION_ARCHITECTURE.md`](./docs/ORCHESTRATION_ARCHITECTURE.md) — what
  the orchestration layer is and how it works.
- [`docs/SCENARIOS.md`](./docs/SCENARIOS.md) — the three required scenarios
  (greenfield/brownfield/ambiguous), each driven through the orchestrator against
  this repo's real codebase.
- [`docs/TESTING_AND_LIMITATIONS.md`](./docs/TESTING_AND_LIMITATIONS.md) — testing
  approach, known limitations, and deliberate trade-offs.
- [`docs/FINAL_SUMMARY.md`](./docs/FINAL_SUMMARY.md) — plan, rationale, artifacts,
  risks, assumptions.
- [`docs/SETUP.md`](./docs/SETUP.md) — run the backend and the orchestrator.

**Backend docs**
- [`docs/BACKEND.md`](./docs/BACKEND.md) — features and how they work.
- [`docs/schemas.md`](./docs/schemas.md) — database and JSON schemas.
- [`docs/FRONTEND_INTEGRATION.md`](./docs/FRONTEND_INTEGRATION.md) — building a
  frontend against this API.
- [`docs/apiflow.md`](./docs/apiflow.md) — call-by-call flows (sequence diagrams) for
  implementing a frontend: create, redirect, stats/QR/analytics, error handling.
- [`openapi.yml`](./openapi.yml) — formal API contract.

## Tech stack

Java 21, Spring Boot 3.3.5, PostgreSQL, Flyway, Hibernate, Testcontainers (backend) —
Python 3.9+ standard library only (orchestrator). No other runtime dependencies for
the orchestrator; no `pip install` needed to run it.
