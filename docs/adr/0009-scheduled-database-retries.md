# ADR-0009: Scheduled database-driven retries

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
Pending payments must be re-sent automatically until a definite answer, at most 10 times (FR-010, FR-011), at a fixed configurable interval (NFR-004), and must keep being retried after a restart (FR-012). Approved by the engineer on 2026-11-23 for the earlier payment-gateway plan and adopted unchanged for payment-authorization.

## Decision
Chose a scheduled job driven by the database:
- A Spring `@Scheduled` job runs every `gateway.retry.interval` (`PT5S` by default, NFR-004). It picks the pending payments (and, once refunds are specified, pending refunds) whose next retry time has passed and re-sends them with the same gateway ID.
- Every automatic retry counts toward the limit of 10, including one that finds the bank unavailable (approved with the earlier plan). After the 11th bank request without a definite answer, the item becomes `unresolved`.
- The current time comes from an injected `java.time.Clock`. Tests use a controllable clock, switch scheduling off and run the job directly. One integration test runs with scheduling switched on.

Chose this over in-memory timers or an executor per payment, which a restart would lose (FR-012), and over a queue library, which would mean new infrastructure.

## Consequences
Retries survive restarts and are testable without waiting. Only one gateway instance can safely run the job. Revisit if more than one gateway instance runs, because the instances would need to coordinate which one retries.
