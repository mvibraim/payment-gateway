# ADR-0010: HTTP API

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
Merchants interact with the gateway over JSON HTTP. Amounts, timestamps, the request key, error reporting (all validation failures together, FR-004) and status codes need a fixed contract. Approved by the engineer on 2026-11-23 for the earlier payment-gateway plan and adopted unchanged for payment-authorization.

## Decision
Chose the following contract:
- Amounts are integers in the currency's smallest unit (5000 means USD 50.00). Timestamps are ISO-8601 UTC.
- The request key goes in the `Idempotency-Key` header.
- Errors use Spring's built-in RFC 9457 Problem Details (`ProblemDetail`), with a `code` and an `errors` list of `{field, message}`. Chose this over a custom error format.

| Endpoint | Responses |
|---|---|
| `POST /payments` | 201 new payment (authorized, declined or pending) · 200 repeat · 400 validation errors · 409 `in_progress` or `key_conflict` · 503 `bank_unavailable` |
| `GET /payments/{id}` | 200 · 404, also for another merchant's payment |
| `GET /payments?from=&to=&status=` (applies once history is specified) | 200, newest first · 400 invalid filter |
| `POST /payments/{id}/refunds` (applies once refunds are specified) | 201 · 200 repeat · 400 · 404 · 409 `in_progress` or `key_conflict` · 422 `not_refundable` or `exceeds_refundable` · 503 `bank_unavailable` |
| `GET /summaries/daily?date=` (applies once daily summaries are specified) | 200 · 400 invalid date |

## Consequences
Errors use a standard format that Spring produces without extra code, and amounts avoid floating-point rounding. Revisit if a client needs a different contract.
