# ADR-0002: SQLite through plain JDBC

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
Payments, request keys and pending retries must survive restarts (BR-009, FR-012). The duplicate-key check relies on visible SQL and unique indexes. Docker isn't installed, so a database server is impractical. Approved by the engineer on 2026-11-23 for the earlier payment-gateway plan and adopted unchanged for payment-authorization.

## Decision
Chose a SQLite file (`org.xerial:sqlite-jdbc`), accessed with Spring's `JdbcClient` and plain SQL, over JPA/Hibernate, because JPA needs a community-maintained SQLite dialect and hides the SQL that the duplicate-key and refund-limit checks rely on. Chose SQLite over in-memory storage, which loses data and pending retries on restart, and over PostgreSQL, which needs a server (engineer's choice).

## Consequences
No server to run, and the SQL is explicit. SQLite allows one writer at a time (see ADR-0003). Revisit if the project moves to a database server.
