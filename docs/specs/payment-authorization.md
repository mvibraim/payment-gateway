# Payment Authorization — Business Specification

Approved: 2026-11-23 (by the engineer, who said to reuse the decisions approved on 2026-11-21 for `docs/specifications/payment-gateway.md`; not confirmed by stakeholders) · Source: `docs/specifications/payment-gateway-brief.md`

Source references: **Brief § 1 / § 2** = phase rows "1. Core functionality" and "2. Reliability"; **Brief § AC** = "Acceptance scenarios"; **Brief § Scope** = "Scope" line. "Was A-###" in the assumptions table gives the ID the same decision had in `docs/specifications/payment-gateway.md`.

## Problem and desired outcome

**Problem:** Merchants need to take card payments, get a clear authorized or declined result, and look payments up later. Duplicate requests, unavailable banks and lost bank replies must never charge a customer twice or report a charged payment as declined.

**Goal:** A simulated gateway that takes payments, reports each one's result accurately, and charges each payment at most once. It is delivered in two phases, core then reliability, and satisfies the Brief's payment acceptance scenarios.

**Users / interacting systems:**
- **Merchant:** submits and retrieves payments.
- **Customer:** the cardholder being charged; doesn't interact with the gateway directly.
- **Simulated bank:** authorizes or declines payments.

## Scope

**In scope:**
- Phase 1: submitting payments, authorized and declined results, retrieving a payment, rejecting invalid payments.
- Phase 2: duplicate protection, unavailable-bank handling, unknown-outcome handling (automatic retries, retry limit, unresolved status).
- Protecting card details and keeping merchants' payments apart.
- A simulated bank that handles payments (A-010).

**Out of scope:**
- Real money, bank accounts or card data (Brief § Scope).
- Refunds (Brief § 3). They will be specified separately.
- Transaction history and daily summaries (Brief § 3). They will be specified separately.
- Separate capture, settlement, voids and chargebacks (A-006).
- Merchant sign-up and credential management (A-005).
- Currencies other than USD, EUR and GBP, and currency conversion (A-003, A-011).
- Settling unresolved payments with the bank (A-018).

## Functional requirements

| ID | Phase | Observable behavior | Provenance / source | Basis |
|---|---|---|---|---|
| FR-001 | 1 | A merchant can submit a payment with card number, expiry month and year, CVV, amount and currency. | Brief § 1; fields per A-003 | EXPLICIT |
| FR-002 | 1 | A valid payment is sent to the bank, and the merchant receives the payment's ID and its result: **authorized** or **declined**. | Brief § 1; A-004 | EXPLICIT |
| FR-003 | 1 | A merchant can retrieve one of its payments by ID and see its current status and details, with card details limited per BR-005. | Brief § 1; A-004 | EXPLICIT |
| FR-004 | 1 | An invalid payment (BR-008) is rejected. It isn't sent to the bank, all validation failures are reported together, and no payment is recorded. | Brief § 1, § AC; A-003 | EXPLICIT |
| FR-005 | 2 | Each payment request carries a request key chosen by the merchant. A repeat with the same key and the same details returns the existing payment's current result without contacting the bank. | Brief § 2; A-001 | ASSUMPTION |
| FR-006 | 2 | A request that reuses a key with different payment details is rejected, and the existing payment is unchanged. | A-001 | ASSUMPTION |
| FR-007 | 2 | A repeat that arrives while the original is still being processed gets an "in progress" response and causes no extra charge. | A-009 | ASSUMPTION |
| FR-008 | 2 | If the bank is unavailable (the request never reached it), the merchant gets a failure they can retry, not a decline. No charge is made, and a retry with the same key tries the bank again. | Brief § 2, § AC; A-002 | EXPLICIT |
| FR-009 | 2 | If the bank's outcome is unknown (the bank may have processed the request, but no answer came back), the payment is recorded as **pending** and is never reported as declined. | Brief § 2, § AC; A-002 | EXPLICIT |
| FR-010 | 2 | The gateway automatically re-sends a pending payment to the bank as the same request until it gets a definite answer, then updates the payment to authorized or declined. It stops after 10 automatic retries. | A-002, A-016 | ASSUMPTION |
| FR-011 | 2 | After 10 automatic retries without a definite answer, the payment becomes **unresolved**, a final status: it is never reported as declined, no further retries are sent, and a repeat with the same key returns "unresolved". | A-016, A-018 | ASSUMPTION |

Basis is EXPLICIT when the Brief states the behavior itself and approved assumptions only add detail. It is ASSUMPTION when the behavior comes from an approved assumption. Every requirement is tagged with its delivery phase (A-012).

## Business rules and invariants

| ID | Rule or invariant | Provenance / basis |
|---|---|---|
| BR-001 | A payment results in at most one charge, however many times it is repeated or retried, including at the same moment. | Brief § 2, § AC; A-001, A-009 |
| BR-002 | A payment is reported as declined only when the bank declined it. | Brief § AC |
| BR-003 | A decline is final. A declined payment is never retried or changed; the merchant submits a new payment with a new key. | A-008 |
| BR-004 | Merchants can access only their own payments. Another merchant's payment is reported as not found. | A-005 |
| BR-005 | Merchants never see the full card number or the CVV, only the last four digits and the expiry month and year. | Brief § AC; A-007 |
| BR-006 | The CVV is kept only until the bank gives a definite answer for the payment or the gateway gives up retrying (the payment becomes unresolved), then discarded. | A-013, A-019 |
| BR-007 | Each payment has one currency, and there is no conversion. | A-011 |
| BR-008 | A payment is valid only if: the card number is present, digits only, 14–19 digits; the expiry month is 1–12 and the expiry month/year isn't in the past; the CVV is 3–4 digits; the amount is a positive whole number in the currency's smallest unit; and the currency is USD, EUR or GBP. | A-003 |
| BR-009 | Payments and their request keys are kept indefinitely, so a repeated key is always recognized. | A-001, A-017 |
| BR-010 | Request keys are per merchant: the same key from two merchants identifies two separate payments. | A-014 |
| BR-011 | Requests that end without a recorded payment (rejected as invalid, or failed because the bank was unavailable) aren't recorded and don't use up their key. | A-015 |

## Nonfunctional requirements

| ID | Quality expectation or constraint | Provenance / basis |
|---|---|---|
| NFR-001 | Simulated transactions only: no real money, bank accounts or card data. | Brief § Scope |
| NFR-002 | Every bank outcome can be triggered deterministically from test input, so the acceptance scenarios can be repeated. | A-010 |
| NFR-003 | Response time, throughput and availability targets: TBD. | Not in Brief (Q-001) |

## Acceptance scenarios

### AC-001 — Valid payment is authorized and can be retrieved

**Covers:** FR-001, FR-002, FR-003  
**Given** a valid USD 50.00 payment that the bank will authorize  
**When** the merchant submits it and then retrieves it using the returned ID  
**Then** the submission returns "authorized", and retrieval shows the same payment as "authorized" for USD 50.00, with only the card's last four digits and expiry  
**Source:** Brief § AC

### AC-002 — Bank declines a valid payment

**Covers:** FR-002, BR-002, BR-003  
**Given** a valid payment that the bank will decline  
**When** the merchant submits it  
**Then** the result is "declined", retrieval shows "declined", and the status never changes afterward  
**Source:** Brief § 1; A-008

### AC-003 — Invalid payment is rejected without a charge

**Covers:** FR-004, BR-008, BR-011  
**Given** a payment with an expiry date in the past and currency JPY  
**When** the merchant submits it  
**Then** it is rejected with both failures reported, nothing is sent to the bank, and no payment can be retrieved  
**Source:** Brief § AC; A-003, A-015

### AC-004 — The same payment submitted twice is charged once

**Covers:** FR-005, BR-001  
**Given** an authorized payment submitted with key K  
**When** the merchant submits the same payment again with key K  
**Then** the response is the original payment (same ID, "authorized"), the bank isn't contacted again, and the customer is charged once  
**Source:** Brief § AC; A-001

### AC-005 — Key reused with different details

**Covers:** FR-006  
**Given** a USD 50.00 payment submitted with key K  
**When** the merchant submits a USD 80.00 payment with key K  
**Then** the request is rejected and the original payment is unchanged  
**Source:** A-001

### AC-006 — Duplicate arrives while the original is still processing

**Covers:** FR-007, BR-001  
**Given** a payment with key K is waiting for the bank  
**When** the same payment arrives again with key K  
**Then** the second request gets "in progress", and the customer is charged at most once  
**Source:** A-009

### AC-007 — An unavailable bank doesn't cause a decline

**Covers:** FR-008, BR-002, BR-011  
**Given** the bank is unavailable  
**When** the merchant submits a valid payment with key K  
**Then** the merchant gets a failure they can retry, not "declined", and no charge is made. Once the bank is available again, a retry with key K is processed once  
**Source:** Brief § AC; A-002, A-015

### AC-008 — The bank charged but its reply was lost

**Covers:** FR-009, FR-010, BR-001, BR-002  
**Given** the bank charges the card but its reply never reaches the gateway  
**When** the merchant submits the payment and then retrieves it  
**Then** it shows "pending", never "declined". After the gateway's automatic retry it shows "authorized", and the customer was charged exactly once  
**Source:** Brief § AC; A-002, A-010

### AC-009 — The bank never received the request

**Covers:** FR-009, FR-010, BR-001  
**Given** the gateway sent a payment that the bank never received, and no reply came back  
**When** the gateway retries automatically  
**Then** the bank processes the payment once, and it moves from "pending" to the bank's result  
**Source:** A-002, A-010

### AC-010 — Merchant retries while the payment is pending

**Covers:** FR-005, FR-009, BR-001  
**Given** a pending payment submitted with key K  
**When** the merchant submits it again with key K  
**Then** the response is the same payment with status "pending", and no second charge is made  
**Source:** A-001, A-002

### AC-011 — The gateway gives up after 10 retries

**Covers:** FR-010, FR-011, BR-002, BR-006  
**Given** a pending payment for which the bank never gives a definite answer  
**When** the gateway's 10th automatic retry also gets no definite answer  
**Then** the payment shows "unresolved", never "declined". No further retries are sent, and a repeat with the same key returns "unresolved"  
**Source:** A-016, A-018

### AC-012 — Card details are never exposed

**Covers:** BR-005, BR-006  
**Given** any payment  
**When** the merchant submits it or retrieves it  
**Then** no response contains the full card number or the CVV. Card details are limited to the last four digits and the expiry month and year  
**Source:** Brief § AC; A-007, A-013

### AC-013 — Merchants see only their own payments

**Covers:** BR-004  
**Given** payment P belongs to merchant A  
**When** merchant B retrieves P  
**Then** P is reported as not found  
**Source:** A-005

## Constraints and dependencies

**External dependencies / data:** the simulated bank is built as part of this work (A-010). For payments it must:
- authorize or decline them;
- simulate being unavailable, a reply lost after a charge, and a request it never received;
- recognize a repeated request with the same key and return its original result without charging again.

**Delivery expectations:** Phase 1 (core) before Phase 2 (reliability), as tagged on each requirement (Brief; A-012). Phase 3 of the Brief (refunds, transaction history, daily summaries) is left to separate specs that will depend on this one.

## Approved assumptions

All assumptions were approved by the engineer in conversation on 2026-11-21 and carried over on 2026-11-23; none were confirmed by stakeholders.

| ID | Assumption and why it was needed | Approved by / when | Affected requirements |
|---|---|---|---|
| A-001 | Duplicates are identified by a request key the merchant chooses. A repeat returns the original result; the same key with different details is rejected; keys are remembered for as long as the payment is kept. Needed because the Brief doesn't define "same payment". | Engineer, 2026-11-21 (was A-001) | FR-005, FR-006, BR-001, BR-009 |
| A-002 | Bank unavailable: a failure the merchant can retry, no charge, and a retry with the same key tries the bank again. Outcome unknown: the payment is pending, never declined, and the gateway automatically re-sends the same request until it gets a definite answer. Needed to define what "handle correctly" means. | Engineer, 2026-11-21 (was A-002) | FR-008 to FR-010 |
| A-003 | Validation rules as in BR-008, with currencies USD, EUR and GBP. All failures are reported together; rejected payments aren't sent to the bank and aren't recorded. | Engineer, 2026-11-21 (was A-004) | FR-001, FR-004, BR-008 |
| A-004 | Retrieve means one payment by ID. | Engineer, 2026-11-21 (was A-005) | FR-002, FR-003 |
| A-005 | Merchants are isolated from each other. Merchant sign-up and credentials are out of scope. | Engineer, 2026-11-21 (was A-006) | BR-004 |
| A-006 | Authorized means charged, which means successful. There is no capture, settlement, void or chargeback. | Engineer, 2026-11-21 (was A-007) | FR-002 |
| A-007 | Merchants see only the last four digits and the expiry. The full card number and CVV are never shown. | Engineer, 2026-11-21 (was A-008) | BR-005 |
| A-008 | Declines are final. To try again, the merchant submits a new payment with a new key. | Engineer, 2026-11-21 (was A-009) | BR-003 |
| A-009 | A duplicate that arrives while the original is still processing gets "in progress". | Engineer, 2026-11-21 (was A-010) | FR-007, BR-001 |
| A-010 | The simulated bank is built as part of this work. Its outcomes can be chosen deterministically from test input, and it recognizes repeated requests by key. | Engineer, 2026-11-21 (was A-011) | NFR-002, dependencies |
| A-011 | Each payment has one currency, and there is no conversion. | Engineer, 2026-11-21 (was A-012) | BR-007 |
| A-012 | The Brief's phases are the delivery order, and each requirement is tagged with its phase. | Engineer, 2026-11-21 (was A-013) | All FRs |
| A-013 | The CVV is kept only until the bank gives a definite answer, then discarded. Needed because automatic retries (A-002) may have to re-send the CVV. | Engineer, 2026-11-21 (was A-014) | BR-006 |
| A-014 | Request keys are per merchant. Follows from merchant isolation (A-005). | Engineer, 2026-11-21 (was A-017) | BR-010 |
| A-015 | Requests rejected as invalid or failed because the bank was unavailable aren't recorded and don't use up their key. | Engineer, 2026-11-21 (was A-018) | BR-011 |
| A-016 | The gateway stops retrying a pending payment after the original attempt plus 10 automatic retries (11 requests to the bank in total). | Engineer, 2026-11-21 (was A-020) | FR-010, FR-011 |
| A-017 | Payments and their request keys are kept indefinitely; nothing is deleted. | Engineer, 2026-11-21 (was A-021) | BR-009 |
| A-018 | When the gateway gives up, the payment becomes **unresolved**, a final status: never shown as declined, no further retries, and a repeat with the same key returns "unresolved". Settling it with the bank happens outside the system. | Engineer, 2026-11-21 (was A-022) | FR-011 |
| A-019 | When the gateway gives up and a payment becomes unresolved, its CVV is discarded. Needed because A-013 alone would keep the CVV of an unresolved payment indefinitely. | Engineer, 2026-11-21 (was A-023) | BR-006 |

## Open questions and limitations

| ID | Question / uncertainty | Impact | Status |
|---|---|---|---|
| Q-001 | Targets for response time, throughput and availability. | Nonblocking | OPEN |

**Limitation:** decisions from the earlier spec that concern refunds, transaction history and daily summaries (was A-003, A-015, A-016, A-019, and the refund and reporting parts of A-005, A-011, A-018, A-020 and A-022) aren't part of this spec. They belong to the specs for those capabilities.
