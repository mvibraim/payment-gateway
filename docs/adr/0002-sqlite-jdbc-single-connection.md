# ADR-0002: SQLite through JdbcClient with one connection

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
Payments and their request keys are kept indefinitely (BR-009), pending payments must survive a restart (FR-012), and a payment must be charged at most once even when duplicates arrive at the same moment (BR-001). The gateway is a single process with simulated transactions only (NFR-001).

## Decision
Chose a SQLite file accessed through Spring's `JdbcClient` over an in-memory store (loses data on restart) and over JPA (more machinery than a few tables need).

- The file path comes from `gateway.db.path`, default `data/payment-gateway.db`. Tables are defined in one `src/main/resources/schema.sql`, applied at startup with `CREATE TABLE IF NOT EXISTS`.
- The connection pool holds one connection, so writes happen one at a time and SQLite never reports "database is locked".
- No bank call happens inside a database transaction or while holding the connection. Each step (record, update, delete) is its own short transaction.
- Uniqueness rules, such as one payment per (merchant, request key), are enforced by database constraints, not by checks in code.

## Consequences
Data survives restarts with no server to run, and the unique constraint settles simultaneous duplicates. Throughput is limited to one writer, which is acceptable while Q-001 (performance targets) is open. A bank call made inside a transaction would block every other request; S05's concurrency test catches this. Revisit if the gateway must run as several instances or Q-001 sets targets that one writer can't meet.
