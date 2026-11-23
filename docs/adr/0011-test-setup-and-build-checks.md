# ADR-0011: Test setup and build checks

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
Every task runs unit, integration and build checks. The acceptance scenarios include concurrency (FR-007) and restarts (FR-012), which need reliable, non-flaky tests. The project has no style rules yet. Approved by the engineer on 2026-11-23 for the earlier payment-gateway plan and adopted unchanged for payment-authorization.

## Decision
Chose this setup:
- Unit tests (`*Test`) run through Maven Surefire without Spring.
- Integration tests (`*IT`) run through Maven Failsafe. They use MockMvc (`@SpringBootTest` + `@AutoConfigureMockMvc`) against the full application context and a SQLite file. Each test starts with empty gateway and bank tables and no bank outcomes set.
- One smoke test starts a real server (`webEnvironment = RANDOM_PORT`, `server.address=127.0.0.1`) and checks that the app serves a request end to end.
- Concurrency tests start their own threads and use the bank's `HOLD` outcome rather than sleeps. Test classes are never `@Transactional`.
- The only lint is `javac -Xlint:all -Werror`.

Chose MockMvc over real HTTP for every integration test (engineer's choice), because both run the same gateway code, but MockMvc gives clearer failures, opens no port on Windows and is slightly faster. Chose the compiler as the only lint over Checkstyle or Spotless, because the project has no style rules yet.

## Consequences
Integration tests are fast and deterministic; server settings are exercised only by the smoke test. Revisit if a feature depends on server settings (`server.*`) or the team wants formatting rules.
