# Payment Gateway — Business Specification

Approved: 2026-11-21 (by the engineer; not confirmed by stakeholders) · Source: `docs/specifications/payment-gateway-brief.md` (business brief pasted in conversation on 2026-11-20, saved unchanged)

Source references: **Brief § 1 / § 2 / § 3** = phase rows "1. Core functionality", "2. Reliability", "3. New features"; **Brief § AC** = "Acceptance scenarios"; **Brief § Scope** = "Scope" line.

## Problem and desired outcome

**Problem:** Merchants need to take card payments, get a clear authorized or declined result, and look payments up later. Duplicate requests, unavailable banks and lost bank replies must never charge a customer twice or report a charged payment as declined. Merchants also need refunds, transaction history and daily summaries.

**Goal:** A simulated payment gateway, delivered in three phases (core, reliability, new features), that satisfies the Brief's acceptance scenarios.

**Users / interacting systems:**
- **Merchant:** submits and manages payments.
- **Customer:** the cardholder being charged; doesn't interact with the gateway directly.
- **Simulated bank:** authorizes or declines payments and processes refunds.

## Scope

**In scope:**
- Phase 1: submitting payments, authorized and declined results, retrieving payments, rejecting invalid payments.
- Phase 2: duplicate protection, unavailable-bank handling, unknown-outcome handling (automatic retries, retry limit, unresolved status).
- Phase 3: refunds, transaction history, daily summaries.
- A simulated bank (A-011).

**Out of scope:**
- Real money, bank accounts or card data (Brief § Scope).
- Separate capture, settlement, voids and chargebacks (A-007).
- Merchant sign-up and credential management (A-006).
- Currencies other than USD, EUR and GBP, and currency conversion (A-004, A-012).
- Settling unresolved payments and refunds with the bank (A-022).

## Functional requirements

| ID | Phase | Observable behavior | Provenance / source | Basis |
|---|---|---|---|---|
| FR-001 | 1 | A merchant can submit a payment with card number, expiry month and year, CVV, amount and currency. | Brief § 1; fields per A-004 | EXPLICIT |
| FR-002 | 1 | A valid payment is sent to the bank, and the merchant receives the payment's ID and its result: **authorized** or **declined**. | Brief § 1; A-005 | EXPLICIT |
| FR-003 | 1 | A merchant can retrieve one of its payments by ID and see its current status and details, with card details limited per BR-007. | Brief § 1; A-005 | EXPLICIT |
| FR-004 | 1 | An invalid payment (BR-010) is rejected. It isn't sent to the bank, all validation failures are reported together, and no payment is recorded. | Brief § 1, § AC; A-004 | EXPLICIT |
| FR-005 | 2 | Each payment request carries a request key chosen by the merchant. A repeat with the same key and the same details returns the existing payment's current result without contacting the bank. | Brief § 2; A-001 | ASSUMPTION |
| FR-006 | 2 | A request that reuses a key with different payment details is rejected, and the existing payment is unchanged. | A-001 | ASSUMPTION |
| FR-007 | 2 | A repeat that arrives while the original is still being processed gets an "in progress" response and causes no extra charge. | A-010 | ASSUMPTION |
| FR-008 | 2 | If the bank is unavailable (the request never reached it), the merchant gets a failure they can retry, not a decline. No charge is made, and a retry with the same key tries the bank again. | Brief § 2, § AC; A-002 | EXPLICIT |
| FR-009 | 2 | If the bank's outcome is unknown (the bank may have processed the request, but no answer came back), the payment is recorded as **pending** and is never reported as declined. | Brief § 2, § AC; A-002 | EXPLICIT |
| FR-010 | 2 | The gateway automatically re-sends a pending payment to the bank as the same request until it gets a definite answer, then updates the payment to authorized or declined. It stops after 10 automatic retries. | A-002, A-020 | ASSUMPTION |
| FR-018 | 2 | After 10 automatic retries without a definite answer, the payment becomes **unresolved**: it is never reported as declined, no further retries are sent, it can't be refunded, and a repeat with the same key returns "unresolved". | A-020, A-022 | ASSUMPTION |
| FR-011 | 3 | A merchant can refund an authorized payment in full or in part, and can make several refunds on the same payment. | Brief § 3; A-003 | EXPLICIT |
| FR-012 | 3 | Refund requests carry a request key and get the same duplicate protection as payments (FR-005 to FR-007). | A-003 | ASSUMPTION |
| FR-013 | 3 | Refunds go through the bank, with the same handling as payments when the bank is unavailable or the outcome is unknown, including the 10-retry limit (FR-008 to FR-010, FR-018). | A-003, A-020 | ASSUMPTION |
| FR-014 | 3 | A retrieved payment and its history entry show each refund (amount, status: successful, declined, pending or unresolved, and date) and the amount still refundable. A declined refund's amount becomes refundable again. | A-015, A-022 | ASSUMPTION |
| FR-015 | 3 | A refund request is rejected without contacting the bank, and nothing is recorded, if the payment isn't found or isn't authorized, the amount isn't a positive whole number in the payment currency's smallest unit, or it exceeds the refundable amount. The merchant gets the reason. | A-016 | ASSUMPTION |
| FR-016 | 3 | A merchant can view its transaction history: its own payments, newest first, filtered by date range and status (authorized, declined, pending, unresolved). | Brief § 3; A-005, A-019, A-022 | EXPLICIT |
| FR-017 | 3 | A merchant can request a summary for one calendar day (UTC). For each currency it shows the number of authorized, declined, pending and unresolved payments, the total authorized, the total refunded, and the net (authorized minus refunded). | Brief § 3; A-005, A-019, A-022 | EXPLICIT |

Basis is EXPLICIT when the Brief states the behavior itself and approved assumptions only add detail. It is ASSUMPTION when the behavior comes from an approved assumption. Rows are grouped by phase; IDs are stable.

## Business rules and invariants

| ID | Rule or invariant | Provenance / basis |
|---|---|---|
| BR-001 | A payment results in at most one charge, however many times it is repeated or retried, including at the same moment. | Brief § 2, § AC; A-001, A-010 |
| BR-002 | A payment is reported as declined only when the bank declined it. | Brief § AC |
| BR-003 | A decline is final. A declined payment is never retried or changed; the merchant submits a new payment with a new key. | A-009 |
| BR-004 | Only authorized payments can be refunded. "Successful payment" means authorized. | Brief § 3; A-003, A-007 |
| BR-005 | The total of successful, pending and unresolved refunds on a payment never exceeds its authorized amount, even when refund requests arrive at the same moment. | Brief § AC; A-003, A-022 |
| BR-006 | Merchants can access only their own payments, refunds, history and summaries. Another merchant's payment is reported as not found. | A-006 |
| BR-007 | Merchants never see the full card number or the CVV, only the last four digits and the expiry month and year. | Brief § AC; A-008 |
| BR-008 | The CVV is kept only until the bank gives a definite answer for the payment or the gateway gives up retrying (the payment becomes unresolved), then discarded. | A-014, A-023 |
| BR-009 | Each payment has one currency, refunds use the payment's currency, and there is no conversion. | A-012 |
| BR-010 | A payment is valid only if: the card number is present, digits only, 14–19 digits; the expiry month is 1–12 and the expiry month/year isn't in the past; the CVV is 3–4 digits; the amount is a positive whole number in the currency's smallest unit; and the currency is USD, EUR or GBP. | A-004 |
| BR-011 | Payments, refunds and their request keys are kept indefinitely, so a repeated key is always recognized. | A-001, A-021 |
| BR-012 | Request keys are per merchant: the same key from two merchants identifies two separate payments. | A-017 |
| BR-013 | Requests that end without a recorded payment (rejected as invalid, or failed because the bank was unavailable) aren't recorded and don't use up their key. | A-018 |
| BR-014 | A payment counts on the day it was submitted, with its status at the time of the query. A refund counts on the day it was requested. "Total refunded" includes only successful refunds. The history date filter uses the submission date. | A-019 |

## Nonfunctional requirements

| ID | Quality expectation or constraint | Provenance / basis |
|---|---|---|
| NFR-001 | Simulated transactions only: no real money, bank accounts or card data. | Brief § Scope |
| NFR-002 | Every bank outcome can be triggered deterministically from test input, so the acceptance scenarios can be repeated. | A-011 |
| NFR-003 | Response time, throughput and availability targets: TBD. | Not in Brief (Q-002) |

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
**Source:** Brief § 1; A-009

### AC-003 — Invalid payment is rejected without a charge

**Covers:** FR-004, BR-010, BR-013  
**Given** a payment with an expiry date in the past and currency JPY  
**When** the merchant submits it  
**Then** it is rejected with both failures reported, nothing is sent to the bank, and no payment can be retrieved or appears in history  
**Source:** Brief § AC; A-004, A-018

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
**Source:** A-010

### AC-007 — An unavailable bank doesn't cause a decline

**Covers:** FR-008, BR-002, BR-013  
**Given** the bank is unavailable  
**When** the merchant submits a valid payment with key K  
**Then** the merchant gets a failure they can retry, not "declined", and no charge is made. Once the bank is available again, a retry with key K is processed once  
**Source:** Brief § AC; A-002, A-018

### AC-008 — The bank charged but its reply was lost

**Covers:** FR-009, FR-010, BR-001, BR-002  
**Given** the bank charges the card but its reply never reaches the gateway  
**When** the merchant submits the payment and then retrieves it  
**Then** it shows "pending", never "declined". After the gateway's automatic retry it shows "authorized", and the customer was charged exactly once  
**Source:** Brief § AC; A-002, A-011

### AC-009 — The bank never received the request

**Covers:** FR-009, FR-010, BR-001  
**Given** the gateway sent a payment that the bank never received, and no reply came back  
**When** the gateway retries automatically  
**Then** the bank processes the payment once, and it moves from "pending" to the bank's result  
**Source:** A-002, A-011

### AC-010 — Merchant retries while the payment is pending

**Covers:** FR-005, FR-009, BR-001  
**Given** a pending payment submitted with key K  
**When** the merchant submits it again with key K  
**Then** the response is the same payment with status "pending", and no second charge is made  
**Source:** A-001, A-002

### AC-011 — Refunds can't exceed the authorized amount

**Covers:** FR-011, BR-005, FR-015  
**Given** an authorized USD 100.00 payment that already has a successful USD 60.00 refund  
**When** the merchant requests another USD 60.00 refund  
**Then** it is rejected. A following USD 40.00 refund succeeds, bringing the total refunded to USD 100.00  
**Source:** Brief § AC; A-003, A-016

### AC-012 — Refunds still in progress count toward the limit

**Covers:** BR-005, FR-013  
**Given** an authorized USD 100.00 payment with a pending or unresolved USD 60.00 refund  
**When** the merchant requests another USD 60.00 refund  
**Then** it is rejected  
**Source:** A-003, A-022

### AC-013 — Only authorized payments can be refunded

**Covers:** BR-004, FR-015  
**Given** one declined payment and one pending payment  
**When** the merchant requests a refund on either of them  
**Then** the request is rejected and nothing is sent to the bank  
**Source:** A-003, A-007, A-016

### AC-014 — A repeated refund request refunds once

**Covers:** FR-012, BR-005  
**Given** a successful USD 30.00 refund made with key R  
**When** the merchant repeats the refund request with key R  
**Then** the original refund is returned, and the customer is refunded only once  
**Source:** A-003

### AC-015 — Card details are never exposed

**Covers:** BR-007, BR-008  
**Given** any payment  
**When** the merchant submits it, retrieves it, views history or requests a summary  
**Then** no response contains the full card number or the CVV. Card details are limited to the last four digits and the expiry month and year  
**Source:** Brief § AC; A-008, A-014

### AC-016 — Merchants see only their own payments

**Covers:** BR-006, BR-012  
**Given** payment P belongs to merchant A  
**When** merchant B retrieves P, views history or requests a summary  
**Then** P is reported as not found and is left out of B's history and summary  
**Source:** A-006, A-017

### AC-017 — History is filtered and newest first

**Covers:** FR-016, BR-014  
**Given** a merchant with authorized and declined payments on several days  
**When** the merchant views history for a date range, filtered to "declined"  
**Then** only that merchant's declined payments submitted in the range are listed, newest first  
**Source:** Brief § 3; A-005, A-019

### AC-018 — Daily summary per currency

**Covers:** FR-017, BR-014  
**Given** that on 2026-11-02 (UTC) a merchant had: authorized USD payments of 50.00 and 30.00, one declined USD payment, one authorized EUR 20.00 payment, and a successful USD 10.00 refund of a payment from 2026-11-01  
**When** the merchant requests the summary for 2026-11-02  
**Then** USD shows 2 authorized, 1 declined, 0 pending, 0 unresolved, total authorized 80.00, refunded 10.00, net 70.00; EUR shows 1 authorized, total authorized 20.00, net 20.00  
**Source:** Brief § 3; A-005, A-019

### AC-019 — The gateway gives up after 10 retries

**Covers:** FR-010, FR-018, BR-002, BR-004  
**Given** a pending payment for which the bank never gives a definite answer  
**When** the gateway's 10th automatic retry also gets no definite answer  
**Then** the payment shows "unresolved", never "declined". No further retries are sent, a refund request on it is rejected, and a repeat with the same key returns "unresolved"  
**Source:** A-020, A-022

## Constraints and dependencies

**External dependencies / data:** the simulated bank is built as part of this work (A-011). It must:
- authorize or decline payments and process refunds;
- simulate being unavailable, a reply lost after a charge, and a request it never received;
- recognize a repeated request with the same key and return its original result without charging or refunding again.

**Delivery expectations:** three phases in order: 1 core, 2 reliability, 3 new features. Every requirement is tagged with its phase (Brief; A-013).

## Approved assumptions

All assumptions were approved by the engineer in conversation; none were confirmed by stakeholders.

| ID | Assumption and why it was needed | Approved by / when | Affected requirements |
|---|---|---|---|
| A-001 | Duplicates are identified by a request key the merchant chooses. A repeat returns the original result; the same key with different details is rejected; keys are remembered for as long as the payment is kept. Needed because the Brief doesn't define "same payment". | Engineer, 2026-11-21 | FR-005, FR-006, BR-001, BR-011 |
| A-002 | Bank unavailable: a failure the merchant can retry, no charge, and a retry with the same key tries the bank again. Outcome unknown: the payment is pending, never declined, and the gateway automatically re-sends the same request until it gets a definite answer. Needed to define what "handle correctly" means. | Engineer, 2026-11-21 | FR-008 to FR-010 |
| A-003 | Refunds apply to authorized payments only, may be partial and multiple, and successful plus in-progress refunds can't exceed the authorized amount. They get the same duplicate protection and go through the bank with the same failure handling. | Engineer, 2026-11-21 | FR-011 to FR-013, BR-004, BR-005 |
| A-004 | Validation rules as in BR-010, with currencies USD, EUR and GBP. All failures are reported together; rejected payments aren't sent to the bank and aren't recorded. | Engineer, 2026-11-21 | FR-001, FR-004, BR-010 |
| A-005 | Retrieve means one payment by ID. History is the merchant's own payments, newest first, filtered by date range and status. The daily summary is generated on request, per currency, with the contents in FR-017; events count on the day they happen; days are in UTC. | Engineer, 2026-11-21 | FR-003, FR-016, FR-017 |
| A-006 | Merchants are isolated from each other. Merchant sign-up and credentials are out of scope. | Engineer, 2026-11-21 | BR-006 |
| A-007 | Authorized means charged, which means successful. There is no capture, settlement, void or chargeback. | Engineer, 2026-11-21 | BR-004 |
| A-008 | Merchants see only the last four digits and the expiry. The full card number and CVV are never shown. The original clause "the CVV isn't kept after the bank request" is replaced by A-014. | Engineer, 2026-11-21 | BR-007 |
| A-009 | Declines are final. To try again, the merchant submits a new payment with a new key. | Engineer, 2026-11-21 | BR-003 |
| A-010 | A duplicate that arrives while the original is still processing gets "in progress". | Engineer, 2026-11-21 | FR-007 |
| A-011 | The simulated bank is built as part of this work. Its outcomes can be chosen deterministically from test input, and it recognizes repeated requests by key. | Engineer, 2026-11-21 | NFR-002, dependencies |
| A-012 | Each payment has one currency, there is no conversion, and summaries are per currency. | Engineer, 2026-11-21 | BR-009, FR-017 |
| A-013 | The phases are the delivery order, and each requirement is tagged with its phase. | Engineer, 2026-11-21 | All FRs |
| A-014 | The CVV is kept only until the bank gives a definite answer, then discarded. Needed because automatic retries (A-002) may have to re-send the CVV, which conflicted with A-008's original clause. | Engineer, 2026-11-21 | BR-008 |
| A-015 | A retrieved payment and its history entry show each refund (amount, status, date) and the amount still refundable. A declined refund's amount becomes refundable again. | Engineer, 2026-11-21 | FR-014 |
| A-016 | Refund requests are rejected without contacting the bank, and nothing is recorded, when the payment isn't found or isn't authorized, the amount isn't a positive whole number in the payment currency's smallest unit, or it exceeds the refundable amount. The merchant gets the reason. | Engineer, 2026-11-21 | FR-015 |
| A-017 | Request keys are per merchant. Follows from merchant isolation (A-006). | Engineer, 2026-11-21 | BR-012 |
| A-018 | Requests rejected as invalid or failed because the bank was unavailable aren't recorded (not in retrieval, history or summaries) and don't use up their key. | Engineer, 2026-11-21 | BR-013 |
| A-019 | Payments count on their submission day with their current status; refunds count on the day requested; "total refunded" includes successful refunds only; history date filtering uses the submission date. | Engineer, 2026-11-21 | BR-014, FR-016, FR-017 |
| A-020 | The gateway stops retrying a pending payment or refund after the original attempt plus 10 automatic retries (11 requests to the bank in total). Closes Q-001. | Engineer, 2026-11-21 | FR-010, FR-013, FR-018 |
| A-021 | Payments, refunds and their request keys are kept indefinitely; nothing is deleted. Closes Q-003. | Engineer, 2026-11-21 | BR-011 |
| A-022 | When the gateway gives up, the payment or refund becomes **unresolved**, a final status: never shown as declined, no further retries, an unresolved payment can't be refunded, and a repeat with the same key returns "unresolved". It appears in history and as its own count in daily summaries. An unresolved refund keeps counting toward the refund limit. Settling it with the bank happens outside the system. | Engineer, 2026-11-21 | FR-014, FR-016 to FR-018, BR-005 |
| A-023 | When the gateway gives up and a payment becomes unresolved, its CVV is discarded. Needed because A-014 alone would keep the CVV of an unresolved payment indefinitely. Closes Q-004. | Engineer, 2026-11-21 | BR-008 |

## Open questions and limitations

| ID | Question / uncertainty | Impact | Status |
|---|---|---|---|
| Q-001 | Should a pending payment or refund ever be given up on, and after how long? | — | CLOSED → A-020, A-022 |
| Q-002 | Targets for response time, throughput and availability. | Nonblocking | OPEN |
| Q-003 | How long payments and their request keys are kept. | — | CLOSED → A-021 |
| Q-004 | Is the CVV discarded when a payment becomes unresolved? | — | CLOSED → A-023 |
