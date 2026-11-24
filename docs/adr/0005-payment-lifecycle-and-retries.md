# ADR-0005: Record before calling the bank, and retry pending payments from the database

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
A payment is charged at most once, including when duplicates arrive at the same moment (BR-001, FR-007). A bank that is unavailable leaves no record and doesn't use up the key (FR-008, BR-011). An unknown outcome makes the payment pending, retried automatically until a definite answer, with 10 automatic retries at most, then unresolved (FR-009 to FR-011). Retries continue after a restart (FR-012) at a fixed, configurable interval (NFR-003). The full card number and CVV are kept only until the payment is final (BR-006, BR-013).

## Decision
Chose to record the payment before calling the bank, with a database-driven retry job, over in-memory locks and in-memory retry timers, which don't survive a restart.

- A new payment is inserted as `in_progress`, together with its request key, detail fingerprint, full card number and CVV, before the bank is called. A unique constraint on (merchant_id, request_key) means a simultaneous duplicate fails to insert. It then reads the existing payment: `in_progress` gives 409 `in_progress`, a different fingerprint gives 409 `key_reused`, otherwise it returns the existing payment.
- The fingerprint is an HMAC-SHA256 of the card number, expiry, amount and currency, keyed by `gateway.fingerprint-secret`. The CVV isn't part of it (BR-014).
- After the bank call: a definite answer sets `authorized` or `declined` and clears the full card number and CVV. "Unavailable" deletes the row. "No reply" sets `pending` with the next attempt time.
- A `@Scheduled` fixed-delay job selects pending payments whose next attempt is due, re-sends each with the same payment ID, and counts automatic retries. A definite answer sets the final status and clears the card data. After the 10th automatic retry without an answer (11 bank requests in all), the payment becomes `unresolved` and its card data is cleared.
- The interval is `gateway.retry.interval`, default 5 seconds.
- At startup, any payment still `in_progress` (its process stopped mid-call) becomes `pending`, so the job picks it up.

## Consequences
At-most-once charging rests on the unique constraint plus the bank recognizing the payment ID. Pending payments resume after a restart with no extra mechanism. The full card number and CVV sit in the database while a payment is in progress or pending, which is acceptable for simulated data (NFR-001). Retries can lag by up to one interval. Revisit if the gateway runs as several instances (the job would need to claim payments) or retries need backoff.
