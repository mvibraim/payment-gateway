# ADR-0003: Simultaneous requests are serialized at the database

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
A payment must result in at most one charge, even when identical requests arrive at the same moment (BR-001, FR-007). The simulated bank writes to the same SQLite database as the gateway (ADR-0005). Approved by the engineer on 2026-11-23 for the earlier payment-gateway plan and adopted unchanged for payment-authorization.

## Decision
Chose a connection pool that holds exactly one connection (Hikari `maximum-pool-size=1`), so database transactions run one at a time, backed by unique indexes on (merchant, request key), with the bank never called inside a database transaction. Chose this over a larger pool with SQLite busy-timeout and immediate-mode transactions, because that can fail with "database busy" errors and is harder to reason about. Keeping the bank call outside transactions also prevents a deadlock, because the simulated bank writes to the same database.

## Consequences
Concurrency behavior is simple and deterministic, but throughput is limited, and there is no target to check against while NFR-003 is TBD. Revisit if NFR-003 sets throughput targets or a second gateway instance is needed.
