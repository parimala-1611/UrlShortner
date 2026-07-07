# Setup Instructions

Two independent things to run: the URL shortener backend (Java/Spring Boot), and the
orchestration engine (Python) that governs changes to it.

## 1. The backend

### Prerequisites

- JDK 21 (the project targets Java 21; `JAVA_HOME` must point to it)
- Docker (for a local PostgreSQL instance, and for Testcontainers-based integration tests)

### Run it locally

```bash
docker compose up -d          # starts local Postgres
./mvnw spring-boot:run         # mvnw.cmd on Windows
```

The app listens on `http://localhost:8080`. See [`BACKEND.md`](./BACKEND.md) for
feature behavior, [`schemas.md`](./schemas.md) for data shapes, and
[`../openapi.yml`](../openapi.yml) for the formal API contract.

### Run the tests

```bash
./mvnw test
```

Note: a handful of tests (`ShortUrlEndToEndTest`, `ShortUrlConcurrencyStressTest`,
`UrlShortenerApplicationTests`) use Testcontainers and require a working Docker
daemon reachable by the Testcontainers client library — see
[`TESTING_AND_LIMITATIONS.md`](./TESTING_AND_LIMITATIONS.md) if these fail locally
despite Docker appearing to run.

## 2. The orchestration engine

### Prerequisites

- Python 3.9+, standard library only — **no `pip install` needed**.
- The backend's prerequisites above (the `test` stage runner actually invokes `mvnw test`).

### Run a scenario end-to-end

From the repo root:

```bash
python orchestrator/run.py start --pipeline orchestrator/pipelines/greenfield.json --run-id my-run
```

If a stage requires approval, the run halts and prints the exact resume command:

```bash
python orchestrator/run.py approve my-run <stage> --pipeline orchestrator/pipelines/greenfield.json --approver "<your name>" --note "<why>"
```

Check status or metrics at any time:

```bash
python orchestrator/run.py status my-run
python orchestrator/run.py report my-run
python orchestrator/run.py report --all      # aggregate across every run
```

The three pipelines already committed to this repo
(`orchestrator/pipelines/{greenfield,brownfield,ambiguous}.json`) reference the
requirements/design artifacts and expected file paths for the features already built —
running them again with a fresh `--run-id` will simply re-validate that everything
still passes (tests still green, docs still present, no new compliance violations)
against the current `HEAD`, since no `--base-ref` is required for a re-run against the
current state of the repo.

### Run the orchestrator's own test suite

```bash
python -m unittest discover -s orchestrator/tests -t .
```

19 tests, no external services required (a throwaway git repo is created per test that
needs one). See [`TESTING_AND_LIMITATIONS.md`](./TESTING_AND_LIMITATIONS.md) for what
these actually verify.

## Repo layout at a glance

```
src/                     Spring Boot backend (Java 21)
docs/                    All narrative documentation (this file, architecture,
                         scenarios, schemas, testing/limitations, final summary)
openapi.yml              Formal API contract
orchestrator/            The orchestration engine (Python, stdlib only)
  pipelines/*.json       The three scenario pipeline specs
  artifacts/             Requirements/design write-ups + release manifests (tracked)
  runs/                  Per-run audit logs (generated, gitignored)
  tests/                 The engine's own unit test suite
```
