# ADR-0004: In-process simulated bank driven by the card number

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
The simulated bank is built as part of this work (A-010). It must authorize or decline, simulate being unavailable, a reply lost after a charge and a request it never received, and recognize a repeated request without charging again. Every outcome must be triggered deterministically from test input (NFR-002), and tests must be able to check that a customer was charged exactly once (BR-001). The gateway may restart while payments are pending (FR-012).

## Decision
Chose a bank that runs inside the gateway process over a separate HTTP service, because it needs no second process to run or test and still exercises every outcome.

- The gateway calls it through a `Bank` interface. Each request is identified by the payment ID, and a repeat with the same payment ID returns the original result without charging again.
- The bank keeps its own record of requests and charges in a SQLite table that the gateway's code never reads, so it still recognizes repeats after a restart. Tests read it to check "charged once".
- The card number's last digit picks the outcome:

| Last digit | Outcome |
|---|---|
| 1 | Declined |
| 2 | Unavailable the first time the bank sees that card number (remembered in memory), then authorized |
| 3 | Charges, but the reply is lost on the first attempt; a repeat returns "authorized" |
| 4 | Never receives the first attempt (no charge, no reply); a repeat is authorized |
| 5 | Never answers (no charge, no reply) |
| 6 | Authorized after a delay set by `bank.slow-delay` |
| any other | Authorized |

- "Unavailable" raises an error meaning the request never reached the bank. "No reply" raises an error meaning the outcome is unknown.

## Consequences
Tests and manual runs trigger every outcome with ordinary card numbers. Because the "unavailable once" memory isn't stored, a card ending in 2 fails once more after a restart. This affects only the simulator. Replacing it with a real bank client means implementing `Bank`. Revisit if the bank must run as a separate service or outcomes must be chosen by something other than the card number.
