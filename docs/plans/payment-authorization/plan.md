# Plan: payment-authorization

Approved: 2026-11-23 at commit `a90be51a8d57feb56fa925bb6535e658b01d92b1` · Branch: `feat/payment-authorization`
Sources: `docs/specs/payment-authorization.md` (approved 2026-11-23), which is based on `docs/specifications/payment-gateway-brief.md`; engineer decisions of 2026-11-23, recorded in ADR-0001 to ADR-0011

**Source notation:** "Spec § FR / § BR / § NFR / § AC" are the spec sections "Functional requirements", "Business rules and invariants", "Nonfunctional requirements" and "Acceptance scenarios". The provenance labels that follow ("Brief § …", "A-###") are copied unchanged from the spec. FR rows keep the spec's own basis label. The spec gives no basis label for BR, NFR and AC rows, so this plan derives one with the spec's own rule: a row that cites the Brief is `EXPLICIT`; a row that cites only approved assumptions is `ASSUMPTION`. Items marked "Engineer, 2026-11-23" were proposed during planning of the earlier payment-gateway plan, approved by the engineer, and carried over into this plan. They aren't in the spec.

## Goal and scope
**Goal:** Build Phase 1 (core) and Phase 2 (reliability) of the simulated payment gateway described in the spec. It is a Java/Spring Boot JSON HTTP API with a SQLite database and an in-process simulated bank.

**In scope:** FR-001 to FR-013, BR-001 to BR-013, NFR-001, NFR-002, NFR-004 and AC-001 to AC-016, as listed below.

**Out of scope:**
- Everything the spec lists as out of scope: real money, bank accounts or card data; refunds; transaction history and daily summaries; separate capture, settlement, voids and chargebacks; merchant sign-up and credential management; currencies other than USD, EUR and GBP, and currency conversion; settling unresolved payments with the bank.
- NFR-003 (response time, throughput and availability targets): still TBD in the spec (Q-001 open). No task covers it.
- CI pipelines, containers and authentication.

## Test commands
All are created by S01. Run them from the repository root. In Git Bash use `./mvnw`; in PowerShell use `.\mvnw.cmd`.
- Unit: `./mvnw -B test` (created by S01). Runs the JUnit 5 tests named `*Test`, without Spring.
- Integration: `./mvnw -B verify` (created by S01). Also runs the `*IT` tests through Maven Failsafe: MockMvc tests against the full application context with a SQLite file, plus one smoke test on a real server bound to `127.0.0.1`. This command reruns the unit tests too.
- Build / lint: `./mvnw -B verify` (created by S01). Compiles with `-Xlint:all -Werror`, so any compiler warning fails the build, and packages the jar.

## Requirements
| ID | Requirement | Source | Basis |
|---|---|---|---|
| FR-001 | Phase 1. A merchant can submit a payment with card number, expiry month and year, CVV, amount and currency. | Spec § FR; Brief § 1; fields per A-003 | EXPLICIT |
| FR-002 | Phase 1. A valid payment is sent to the bank, and the merchant receives the payment's ID and its result: **authorized** or **declined**. | Spec § FR; Brief § 1; A-004 | EXPLICIT |
| FR-003 | Phase 1. A merchant can retrieve one of its payments by ID and see its current status and details, with card details limited per BR-005. | Spec § FR; Brief § 1; A-004 | EXPLICIT |
| FR-004 | Phase 1. An invalid payment (BR-008) is rejected. It isn't sent to the bank, all validation failures are reported together, and no payment is recorded. | Spec § FR; Brief § 1, § AC; A-003 | EXPLICIT |
| FR-005 | Phase 2. Each payment request carries a request key chosen by the merchant. A repeat with the same key and the same details returns the existing payment's current result without contacting the bank. | Spec § FR; Brief § 2; A-001 | ASSUMPTION |
| FR-006 | Phase 2. A request that reuses a key with different payment details is rejected, and the existing payment is unchanged. | Spec § FR; A-001 | ASSUMPTION |
| FR-007 | Phase 2. A repeat that arrives while the original is still being processed gets an "in progress" response and causes no extra charge. | Spec § FR; A-009 | ASSUMPTION |
| FR-008 | Phase 2. If the bank is unavailable (the request never reached it), the merchant gets a failure they can retry, not a decline. No charge is made, and a retry with the same key tries the bank again. | Spec § FR; Brief § 2, § AC; A-002 | EXPLICIT |
| FR-009 | Phase 2. If the bank's outcome is unknown (the bank may have processed the request, but no answer came back), the payment is recorded as **pending** and is never reported as declined. | Spec § FR; Brief § 2, § AC; A-002 | EXPLICIT |
| FR-010 | Phase 2. The gateway automatically re-sends a pending payment to the bank as the same request until it gets a definite answer, then updates the payment to authorized or declined. It stops after 10 automatic retries. | Spec § FR; A-002, A-016 | ASSUMPTION |
| FR-011 | Phase 2. After 10 automatic retries without a definite answer, the payment becomes **unresolved**, a final status: it is never reported as declined, no further retries are sent, and a repeat with the same key returns "unresolved". | Spec § FR; A-016, A-018 | ASSUMPTION |
| FR-012 | Phase 2. Pending payments keep being retried after a gateway restart, within the same 10-retry limit. | Engineer, 2026-11-23 | ASSUMPTION |
| FR-013 | Phase 2. Payment requests must carry a request key. A missing or empty key, or one longer than 255 characters, is reported together with the other validation failures. | Engineer, 2026-11-23 | ASSUMPTION |
| BR-001 | A payment results in at most one charge, however many times it is repeated or retried, including at the same moment. | Spec § BR; Brief § 2, § AC; A-001, A-009 | EXPLICIT |
| BR-002 | A payment is reported as declined only when the bank declined it. | Spec § BR; Brief § AC | EXPLICIT |
| BR-003 | A decline is final. A declined payment is never retried or changed; the merchant submits a new payment with a new key. | Spec § BR; A-008 | ASSUMPTION |
| BR-004 | Merchants can access only their own payments. Another merchant's payment is reported as not found. | Spec § BR; A-005 | ASSUMPTION |
| BR-005 | Merchants never see the full card number or the CVV, only the last four digits and the expiry month and year. | Spec § BR; Brief § AC; A-007 | EXPLICIT |
| BR-006 | The CVV is kept only until the bank gives a definite answer for the payment or the gateway gives up retrying (the payment becomes unresolved), then discarded. | Spec § BR; A-013, A-019 | ASSUMPTION |
| BR-007 | Each payment has one currency, and there is no conversion. | Spec § BR; A-011 | ASSUMPTION |
| BR-008 | A payment is valid only if: the card number is present, digits only, 14–19 digits; the expiry month is 1–12 and the expiry month/year isn't in the past; the CVV is 3–4 digits; the amount is a positive whole number in the currency's smallest unit; and the currency is USD, EUR or GBP. | Spec § BR; A-003 | ASSUMPTION |
| BR-009 | Payments and their request keys are kept indefinitely, so a repeated key is always recognized. | Spec § BR; A-001, A-017 | ASSUMPTION |
| BR-010 | Request keys are per merchant: the same key from two merchants identifies two separate payments. | Spec § BR; A-014 | ASSUMPTION |
| BR-011 | Requests that end without a recorded payment (rejected as invalid, or failed because the bank was unavailable) aren't recorded and don't use up their key. | Spec § BR; A-015 | ASSUMPTION |
| BR-012 | A card is valid through the last day of its expiry month, judged in UTC, so a card expiring in the current month is accepted. | Engineer, 2026-11-23 | ASSUMPTION |
| BR-013 | The full card number is discarded at the same moment as the CVV, keeping only the last four digits. | Engineer, 2026-11-23 | ASSUMPTION |
| NFR-001 | Simulated transactions only: no real money, bank accounts or card data. | Spec § NFR; Brief § Scope | EXPLICIT |
| NFR-002 | Every bank outcome can be triggered deterministically from test input, so the acceptance scenarios can be repeated. | Spec § NFR; A-010 | ASSUMPTION |
| NFR-004 | Automatic retries happen at a fixed, configurable interval, 5 seconds by default. | Engineer, 2026-11-23 | ASSUMPTION |

| AC | Covers | Given / When / Then | Basis |
|---|---|---|---|
| AC-001 | FR-001, FR-002, FR-003 | Given a valid USD 50.00 payment that the bank will authorize, when the merchant submits it and then retrieves it using the returned ID, then the submission returns "authorized", and retrieval shows the same payment as "authorized" for USD 50.00, with only the card's last four digits and expiry. (Spec § AC; Brief § AC) | EXPLICIT |
| AC-002 | FR-002, BR-002, BR-003 | Given a valid payment that the bank will decline, when the merchant submits it, then the result is "declined", retrieval shows "declined", and the status never changes afterward. (Spec § AC; Brief § 1; A-008) | EXPLICIT |
| AC-003 | FR-004, BR-008, BR-011 | Given a payment with an expiry date in the past and currency JPY, when the merchant submits it, then it is rejected with both failures reported, nothing is sent to the bank, and no payment can be retrieved. (Spec § AC; Brief § AC; A-003, A-015) | EXPLICIT |
| AC-004 | FR-005, BR-001 | Given an authorized payment submitted with key K, when the merchant submits the same payment again with key K, then the response is the original payment (same ID, "authorized"), the bank isn't contacted again, and the customer is charged once. (Spec § AC; Brief § AC; A-001) | EXPLICIT |
| AC-005 | FR-006 | Given a USD 50.00 payment submitted with key K, when the merchant submits a USD 80.00 payment with key K, then the request is rejected and the original payment is unchanged. (Spec § AC; A-001) | ASSUMPTION |
| AC-006 | FR-007, BR-001 | Given a payment with key K is waiting for the bank, when the same payment arrives again with key K, then the second request gets "in progress", and the customer is charged at most once. (Spec § AC; A-009) | ASSUMPTION |
| AC-007 | FR-008, BR-002, BR-011 | Given the bank is unavailable, when the merchant submits a valid payment with key K, then the merchant gets a failure they can retry, not "declined", and no charge is made. Once the bank is available again, a retry with key K is processed once. (Spec § AC; Brief § AC; A-002, A-015) | EXPLICIT |
| AC-008 | FR-009, FR-010, BR-001, BR-002 | Given the bank charges the card but its reply never reaches the gateway, when the merchant submits the payment and then retrieves it, then it shows "pending", never "declined". After the gateway's automatic retry it shows "authorized", and the customer was charged exactly once. (Spec § AC; Brief § AC; A-002, A-010) | EXPLICIT |
| AC-009 | FR-009, FR-010, BR-001 | Given the gateway sent a payment that the bank never received, and no reply came back, when the gateway retries automatically, then the bank processes the payment once, and it moves from "pending" to the bank's result. (Spec § AC; A-002, A-010) | ASSUMPTION |
| AC-010 | FR-005, FR-009, BR-001 | Given a pending payment submitted with key K, when the merchant submits it again with key K, then the response is the same payment with status "pending", and no second charge is made. (Spec § AC; A-001, A-002) | ASSUMPTION |
| AC-011 | FR-010, FR-011, BR-002, BR-006 | Given a pending payment for which the bank never gives a definite answer, when the gateway's 10th automatic retry also gets no definite answer, then the payment shows "unresolved", never "declined". No further retries are sent, and a repeat with the same key returns "unresolved". (Spec § AC; A-016, A-018) | ASSUMPTION |
| AC-012 | BR-005, BR-006 | Given any payment, when the merchant submits it or retrieves it, then no response contains the full card number or the CVV. Card details are limited to the last four digits and the expiry month and year. (Spec § AC; Brief § AC; A-007, A-013) | EXPLICIT |
| AC-013 | BR-004 | Given payment P belongs to merchant A, when merchant B retrieves P, then P is reported as not found. (Spec § AC; A-005) | ASSUMPTION |
| AC-014 | FR-012 | Given a pending payment, when the gateway restarts, then automatic retries continue and the payment changes to the bank's result. (Engineer, 2026-11-23) | ASSUMPTION |
| AC-015 | FR-013 | Given a payment request with no request key and an invalid CVV, when it is submitted, then both failures are reported and nothing is sent to the bank. (Engineer, 2026-11-23) | ASSUMPTION |
| AC-016 | BR-012 | Given today is 2026-11-21 (UTC), when payments are submitted, then one expiring 11/2026 is accepted and one expiring 10/2026 is rejected. (Engineer, 2026-11-23) | ASSUMPTION |

## Decisions
All were approved by the engineer on 2026-11-23 for the earlier payment-gateway plan and adopted unchanged for this plan. Each ADR records the whole decision, including parts that apply only once refunds, transaction history or daily summaries are specified; this plan implements only the payment parts.
- [ADR-0001: Language, framework and build tool](../../adr/0001-java-spring-boot-maven.md), new
- [ADR-0002: SQLite through plain JDBC](../../adr/0002-sqlite-plain-jdbc.md), new
- [ADR-0003: Simultaneous requests are serialized at the database](../../adr/0003-serialize-database-transactions.md), new
- [ADR-0004: One schema file, no migrations](../../adr/0004-single-schema-file.md), new
- [ADR-0005: In-process simulated bank whose outcomes tests set](../../adr/0005-in-process-simulated-bank.md), new
- [ADR-0006: Merchant identity from a trusted header](../../adr/0006-merchant-id-header.md), new
- [ADR-0007: What counts as a repeat](../../adr/0007-repeat-matching.md), new
- [ADR-0008: Record first, then call the bank](../../adr/0008-record-before-bank-call.md), new
- [ADR-0009: Scheduled database-driven retries](../../adr/0009-scheduled-database-retries.md), new
- [ADR-0010: HTTP API](../../adr/0010-http-api.md), new
- [ADR-0011: Test setup and build checks](../../adr/0011-test-setup-and-build-checks.md), new

## Tasks
Tasks are grouped by spec phase and listed in execution order. Every task runs all three test commands. Each task updates the README's API section for the endpoints it adds or changes.

### Phase 1: core

### S01: Project setup
- **Status:** `NOT STARTED`
- **Objective:** Create a buildable Spring Boot project with working unit and integration suites, so every later task can add tests.
- **Covers:** none (setup task)
- **Depends on:** none
- **Scope:**
  - `pom.xml`: Spring Boot 4.x parent, Java 25, the web, JDBC and test starters, `sqlite-jdbc`, the Failsafe plugin, and compiler arguments `-Xlint:all -Werror`.
  - Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, scripts-only type).
  - The application class in package `com.example.gateway`.
  - `application.properties`: database file `./data/gateway.db`, `maximum-pool-size=1`, schema initialization from `schema.sql`, `gateway.retry.interval=PT5S`.
  - An empty `schema.sql`, a `Clock` bean (system UTC) and `.gitignore` (`target/`, `data/`).
  - Shared integration-test setup: a temporary SQLite file per test run, and MockMvc.
  - One example unit test, one MockMvc integration test that loads the context, and one real-server smoke test bound to `127.0.0.1`.
  - README: prerequisites (JDK 25), how to run, the test commands and configuration.
  - Not in this task: any payment behavior.
- **Extra checks:** `./mvnw spring-boot:run` starts the app and creates `data/gateway.db`. Then stop it.
- **Commit:** `build: set up Spring Boot project with SQLite and test suites`

### S02: Submit a payment to the simulated bank
- **Status:** `NOT STARTED`
- **Objective:** A merchant can submit a payment and get its ID and the bank's result.
- **Covers:** FR-001, FR-002, BR-002, BR-006 (discard on a definite answer), BR-013, NFR-001, NFR-002; AC-002 (the submit part)
- **Depends on:** S01
- **Scope:**
  - `POST /payments` with the required `X-Merchant-Id` header (ADR-0006). A malformed JSON body gets 400.
  - The payment is inserted as `processing` (ADR-0008), sent to `SimulatedBank` with the payment ID as its key, and updated to `authorized` or `declined`. The CVV and full card number are then set to null, and the last four digits are kept.
  - Returns 201 with the ID, status, amount, currency, card last four digits and expiry, and creation time.
  - `SimulatedBank` with `AUTHORIZE`/`DECLINE` outcomes set per card number (unscripted means authorize), the `bank_payments` table, and call and charge counts for tests (ADR-0005).
  - `payments` and `bank_payments` tables in `schema.sql`.
  - Not in this task: validation rules (S04), request keys (S05), bank faults (S07, S08).
- **Extra checks:** none
- **Commit:** `feat: submit payments to the simulated bank`

### S03: Retrieve a payment
- **Status:** `NOT STARTED`
- **Objective:** A merchant can retrieve one of its own payments by ID, with masked card details.
- **Covers:** FR-003, BR-004, BR-005; AC-001, AC-002, AC-012, AC-013
- **Depends on:** S02
- **Scope:**
  - `GET /payments/{id}`. Another merchant's payment, or an unknown ID, returns 404.
  - The response shape is the same as for submission. `processing` is shown as `pending`.
  - Tests check that no response body contains the full card number or the CVV.
- **Extra checks:** none
- **Commit:** `feat: retrieve a payment with masked card details`

### S04: Reject invalid payments
- **Status:** `NOT STARTED`
- **Objective:** Invalid payments are rejected with every failure reported, without contacting the bank or recording anything.
- **Covers:** FR-004, BR-008, BR-011, BR-012; AC-003, AC-016
- **Depends on:** S02
- **Scope:**
  - A validator that collects all failures for BR-008 and BR-012, using the injected `Clock`.
  - 400 Problem Details with the `errors` list. No bank call and no row inserted.
  - Unit tests for each rule and its boundaries. Integration tests for AC-003 and AC-016.
- **Extra checks:** none
- **Commit:** `feat: reject invalid payments with all validation failures`

### Phase 2: reliability

### S05: Request keys and repeated payments
- **Status:** `NOT STARTED`
- **Objective:** A repeated payment request with the same key never causes a second charge.
- **Covers:** FR-005, FR-006, FR-013, BR-001, BR-009, BR-010; AC-004, AC-005, AC-015
- **Depends on:** S04
- **Scope:**
  - A required `Idempotency-Key` header. A missing or empty key, or one longer than 255 characters, is added to the validation failures.
  - The key is stored on the payment, with a unique index on (merchant, key).
  - A repeat with the same details (ADR-0007) returns 200 with the current payment and makes no bank call.
  - A repeat with different details returns 409 `key_conflict`, and the original payment is unchanged.
  - The same key from two merchants creates two payments.
  - Nothing is ever deleted except the unavailable-bank case (S07).
  - Not in this task: repeats that arrive while the original is processing (S06).
- **Extra checks:** none
- **Commit:** `feat: deduplicate payments by request key`

### S06: Duplicate arrives while the original is processing
- **Status:** `NOT STARTED`
- **Objective:** A repeat that arrives while the original is still waiting for the bank gets "in progress" and causes no extra charge.
- **Covers:** FR-007, BR-001 (requests at the same moment); AC-006
- **Depends on:** S05
- **Scope:**
  - A repeat of a payment that is still `processing` returns 409 `in_progress`.
  - Adds the `HOLD` outcome to `SimulatedBank`.
  - Integration tests send the original on a test-started thread and hold it at the bank, send the repeat, release the original, then check one charge. Another test sends parallel identical requests and checks one charge.
- **Extra checks:** Run the concurrency integration tests 20 times in a row; all runs must pass.
- **Commit:** `feat: return in progress for concurrent duplicate payments`

### S07: The bank is unavailable
- **Status:** `NOT STARTED`
- **Objective:** An unavailable bank produces a failure the merchant can retry, never a decline, and doesn't use up the key.
- **Covers:** FR-008, BR-002, BR-011; AC-007
- **Depends on:** S05
- **Scope:** the `UNAVAILABLE` bank outcome. The `processing` row is deleted and the merchant gets 503 `bank_unavailable`. A retry with the same key reaches the bank and is processed once.
- **Extra checks:** none
- **Commit:** `feat: report an unavailable bank as a retryable failure`

### S08: The bank's outcome is unknown
- **Status:** `NOT STARTED`
- **Objective:** A payment whose bank outcome is unknown is recorded as pending and never reported as declined.
- **Covers:** FR-009, BR-002; AC-010, AC-008 (the "pending" part)
- **Depends on:** S05
- **Scope:**
  - `REPLY_LOST` (the bank records and charges, then throws "outcome unknown") and `NOT_RECEIVED` (throws without processing) bank outcomes.
  - The payment becomes `pending`, with an attempt count and next retry time stored; the CVV and card number are kept for retries.
  - Returns 201 with status `pending`. A repeat with the same key returns 200 `pending`.
  - Not in this task: automatic retries (S09).
- **Extra checks:** none
- **Commit:** `feat: record payments with an unknown bank outcome as pending`

### S09: Retry pending payments automatically
- **Status:** `NOT STARTED`
- **Objective:** The gateway re-sends pending payments until the bank gives a definite answer, without charging twice.
- **Covers:** FR-010, NFR-004, BR-001, BR-003; AC-008, AC-009, AC-002 (the status never changes)
- **Depends on:** S08
- **Scope:**
  - The retry job (ADR-0009) re-sends the same request with the same payment ID.
  - `SimulatedBank` recognizes a known key and returns its original result without charging again.
  - On a definite answer the payment's status is updated, and the CVV and card number are set to null.
  - Authorized and declined payments are never retried.
  - A controllable test clock. One integration test runs with scheduling on and a short interval.
- **Extra checks:** none
- **Commit:** `feat: retry pending payments automatically`

### S10: Give up after 10 automatic retries
- **Status:** `NOT STARTED`
- **Objective:** A payment that never gets a definite answer becomes unresolved after 10 automatic retries.
- **Covers:** FR-011, BR-006, BR-013; AC-011
- **Depends on:** S09
- **Scope:** if the 11th bank request (the original plus 10 retries, each counted whatever its failure) gets no definite answer, the payment becomes `unresolved`. Its CVV and card number are set to null, it is never retried again, and a repeat with the same key returns `unresolved`.
- **Extra checks:** none
- **Commit:** `feat: mark payments unresolved after 10 automatic retries`

### S11: Resume payment retries after a restart
- **Status:** `NOT STARTED`
- **Objective:** Pending payments keep being retried after the gateway restarts.
- **Covers:** FR-012; AC-014
- **Depends on:** S09
- **Scope:**
  - At startup, payments still marked `processing` become `pending` and are due a retry immediately.
  - Retries continue from the stored attempt count.
  - The integration test closes the application context and starts a new one on the same database file, then checks the payment reaches the bank's result with exactly one charge.
- **Extra checks:** none
- **Commit:** `feat: resume pending retries after a restart`

**Execution order:** S01 → S02 → (S03, S04) → S05 → (S06, S07, S08) → S09 → (S10, S11). Tasks in brackets can be done in any order once their dependencies are done.

**Not covered by any task:** NFR-003 (targets TBD, Q-001 open in the spec).

## Risks and limitations
- **Plan size:** 11 tasks in one implementation pull request, more than the usual limit of about eight. Approved by the engineer on 2026-11-23 to follow the earlier payment-gateway plan's task breakdown.
- **Network needed for S01:** Maven isn't installed. The first build downloads Maven and all dependencies.
- **No migrations (ADR-0004):** when `schema.sql` changes, an existing local `data/gateway.db` must be deleted. Tests always start from a new database.
- **Throughput:** the single database connection (ADR-0003) limits how many requests are handled at once. There is no target to check against while NFR-003 is TBD.
- **Single instance only:** two gateway instances would both run the retry job. The bank's repeat recognition still prevents double charges, but status updates could clash.
- **Flaky concurrency tests (S06):** these depend on synchronization. They use the bank's `HOLD` outcome and latches, never sleeps, and their extra checks run them 20 times.
- **No authentication:** any caller can act as any merchant (ADR-0006). This is accepted for a simulation.
- **Card data storage:** the full card number and CVV are stored unencrypted in SQLite while a payment is processing or pending. This is acceptable only because the data is simulated (NFR-001).
- **Shared database:** the simulated bank's tables live in the gateway's database. Resetting the database also resets the bank.
- **ADRs cover more than this plan builds:** the refund, history and summary parts of ADR-0005 and ADR-0007 to ADR-0010 aren't implemented here; they apply once those capabilities are specified.
- **Spec readings approved with the earlier plan (2026-11-23):**
  - Merchants see `processing` as pending (ADR-0008).
  - Automatic retries that find the bank unavailable count toward the limit of 10 (ADR-0009).
