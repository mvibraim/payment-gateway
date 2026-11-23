# ADR-0005: In-process simulated bank whose outcomes tests set

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
The simulated bank is built as part of this work, and every bank outcome must be triggerable deterministically from test input (NFR-002). It must authorize or decline, be unavailable, lose a reply after charging, never receive a request, and recognize a repeated request without charging again. The gateway discards the full card number once a payment is final (BR-013). Approved by the engineer on 2026-11-23 (choice 4a) for the earlier payment-gateway plan and adopted unchanged for payment-authorization.

## Decision
Chose a simulated bank that runs inside the gateway as a Spring bean `SimulatedBank`, with no separate interface because it is the only bank:
- Tests set the outcomes of the next bank calls: `AUTHORIZE`/`SUCCEED`, `DECLINE`, `UNAVAILABLE` (the request never arrives), `REPLY_LOST` (the bank processes it, but no reply comes back), `NOT_RECEIVED` (no processing and no reply), and `HOLD` (blocks the call until the test releases it).
- Payment outcomes are set per card number. With nothing set, payments are authorized.
- The bank records each processed payment in its own `bank_payments` table, keyed by the gateway's payment ID. A repeated request gets its original result without a second charge, and this survives restarts. Tests can read call and charge counts.
- The outcome settings are held in memory, because they are test input.
- Applies once refunds are specified: refund outcomes are set per original payment ID, because the gateway discards the full card number (BR-013); with nothing set, refunds succeed. Processed refunds are recorded in a `bank_refunds` table keyed by the gateway's refund ID, a repeated refund gets its original result without a second refund, and tests can read refund counts.

Chose this over a test-mode HTTP control endpoint (4b), fixed test card numbers (4c, which can't express "fail once, then succeed"), and keeping the bank's records in memory (a restart could let a retried payment be charged twice).

## Consequences
Every acceptance scenario can be driven from a test without a separate service. The bank's tables live in the gateway's database, so resetting the database also resets the bank. Revisit if a separate bank service or a real bank comes into scope.
