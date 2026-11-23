# ADR-0004: One schema file, no migrations

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
Several tasks add tables or columns. Nothing is deployed yet, so no existing data needs to be preserved. Approved by the engineer on 2026-11-23 for the earlier payment-gateway plan and adopted unchanged for payment-authorization.

## Decision
Chose one `schema.sql` file, applied at startup (`CREATE TABLE IF NOT EXISTS`) and edited by each task that needs a new table or column, over Flyway migrations, because nothing is deployed yet and this saves a dependency.

## Consequences
When `schema.sql` changes, an existing local `data/gateway.db` must be deleted; tests always start from a new database. Revisit before the first deployment whose data must be kept: switch to versioned migrations.
