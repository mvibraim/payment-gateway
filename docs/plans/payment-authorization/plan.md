# Plan: payment-authorization

Approved: 2026-11-24 at commit `222c1263b9e03eae41dee1e88c140a273ff4c5cc`
Sources: `docs/specs/payment-authorization.md`

## Goal
**Goal:** Deliver the spec's phase 1 (submit, retrieve, validate: FR-001 to FR-004) and then phase 2 (duplicates, unavailable bank, pending, automatic retries, unresolved: FR-005 to FR-011), with the additions below. The gateway is a Java/Spring Boot JSON HTTP API with a SQLite file and an in-process simulated bank, and charges each payment at most once.
**Out of scope:** Everything the spec lists as out of scope (real money or card data; refunds, transaction history, daily summaries; capture, settlement, voids, chargebacks; merchant sign-up and credentials; other currencies and conversion; settling unresolved payments). No task covers Q-001 (response time, throughput and availability targets), which stays open. CI pipelines and containers.

## Test commands
All are created by S01. Run them from the repository root; in PowerShell use `.\gradlew.bat` instead of `./gradlew`.
- Unit: `./gradlew test` (created by S01): JUnit 5 tests in `src/test/java/com/example/gateway/unit`, without Spring.
- Integration: `./gradlew integrationTest` (created by S01): `src/test/java/com/example/gateway/integration`, full application through MockMvc against a SQLite file in `@TempDir` (ADR-0001).
- Build / lint: `./gradlew build` (created by S01): compiles with `-Xlint:all -Werror`, runs both suites and builds the jar.

## Requirements
Spec items are linked by ID from `docs/specs/payment-authorization.md`. These items are added by this plan, approved by the engineer on 2026-11-23.

| ID | Requirement, or Given / When / Then | Basis |
|---|---|---|
| FR-012 | Pending payments keep being retried after a gateway restart, within the same 10-retry limit. A payment left mid-request by a crash is treated as pending. | ASSUMPTION |
| FR-013 | Payment requests must carry a request key: non-empty, at most 255 characters. A missing, empty or too-long key is reported together with the other validation failures. | ASSUMPTION |
| BR-012 | A card is valid through the last day of its expiry month, judged in UTC. | ASSUMPTION |
| BR-013 | The full card number is discarded at the same moment as the CVV, keeping only the last four digits. | ASSUMPTION |
| BR-014 | "Same details" for a repeated key (FR-005, FR-006) means the same card number, expiry, amount and currency. The CVV isn't compared. | ASSUMPTION |
| NFR-003 | Automatic retries run at a fixed, configurable interval, 5 seconds by default (answers spec Q-002). | ASSUMPTION |
| AC-015 | FR-012: Given a pending payment, or one left in progress by a crash, when the gateway starts on that database, then automatic retries continue within the limit and the payment reaches the bank's result, charged once. | ASSUMPTION |
| AC-016 | FR-013: Given a payment request with no request key and a 2-digit CVV, when it is submitted, then both failures are reported and nothing is sent to the bank. | ASSUMPTION |
| AC-017 | BR-012: Given today is 2026-11-23 (UTC), when payments are submitted, then one expiring 11/2026 is accepted and one expiring 10/2026 is rejected. | ASSUMPTION |
| AC-018 | BR-006, BR-013: Given a payment that got a definite answer or became unresolved, when its stored record is read, then it holds no full card number and no CVV, only the last four digits. | ASSUMPTION |
| AC-019 | BR-014: Given an authorized payment submitted with key K, when the same details are submitted again with key K but a different CVV, then the original payment is returned and the bank isn't contacted. | ASSUMPTION |

## Decisions
- [ADR-0001: Java, Spring Boot and the Gradle wrapper, with separate unit and integration suites](../../adr/0001-java-spring-boot-gradle.md), new
- [ADR-0002: SQLite through JdbcClient with one connection](../../adr/0002-sqlite-jdbc-single-connection.md), new
- [ADR-0003: JSON HTTP API with merchant and request-key headers](../../adr/0003-http-api.md), new
- [ADR-0004: In-process simulated bank driven by the card number](../../adr/0004-in-process-simulated-bank.md), new
- [ADR-0005: Record before calling the bank, and retry pending payments from the database](../../adr/0005-payment-lifecycle-and-retries.md), new

## Tasks
### S01: Project setup
- **Status:** `DONE`
- **Covers:** none (setup task; creates the test commands)
- **Depends on:** none
- **Branch:** `build/payment-authorization-s01-project-setup`
- **Scope:** Per ADR-0001 and ADR-0002: `settings.gradle.kts`, `build.gradle.kts` (Spring Boot web and JDBC starters, `sqlite-jdbc`, JVM Test Suite `integrationTest` wired into `check`, `-Xlint:all -Werror`, Awaitility in the integration suite), Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`) taken from a Spring Initializr project (Gradle, Kotlin DSL) because Gradle isn't installed; application class; SQLite datasource from `gateway.db.path` (default `data/payment-gateway.db`, parent folder created at startup) with a one-connection pool; an empty `schema.sql` applied at startup; one sample unit test and one integration test that the application context starts on a `@TempDir` database; `.gitignore` for `build/`, `.gradle/`, `data/`. Docs: create `README.md` with how to run the gateway and the three test commands.
- **Extra checks:** `./gradlew --version` runs from the wrapper; `java -jar build/libs/<jar>` starts and creates the database file.
- **Commit:** `build: set up Spring Boot project with SQLite and test suites`

### S02: Submit a payment
- **Status:** `DONE`
- **Covers:** FR-001, FR-002, BR-002, BR-005, BR-007, NFR-001, NFR-002; the submit half of AC-001, AC-002, AC-012
- **Depends on:** S01
- **Branch:** `feat/payment-authorization-s02-submit-payment`
- **Scope:** `POST /payments` per ADR-0003 with required `X-Merchant-Id` (400 if missing), body `cardNumber`, `expiryMonth`, `expiryYear`, `cvv`, `amount`, `currency`. `Bank` interface and simulated bank per ADR-0004 with only digit 1 (declined) and the default (authorized) for now, its request/charge table, and repeat recognition by payment ID. `payments` table: UUID ID, merchant ID, last four digits, expiry, amount, currency, status, created time; no full card number or CVV is stored. Call the bank, then record the payment and return 201 with the payment JSON. One `@RestControllerAdvice` for error bodies. No validation yet (S04) and no request keys yet (S05). Docs: README API section (endpoint, headers, request and response, bank digits 1 and default).
- **Extra checks:** curl smoke test against the running jar: one authorized and one declined payment.
- **Commit:** `feat: submit payments to the simulated bank`

### S03: Retrieve a payment
- **Status:** `NOT STARTED`
- **Covers:** FR-003, BR-003, BR-004, BR-005; AC-001, AC-002, AC-012, AC-013
- **Depends on:** S02
- **Branch:** `feat/payment-authorization-s03-retrieve-payment`
- **Scope:** `GET /payments/{id}` per ADR-0003 with required `X-Merchant-Id`: 200 with the payment JSON (card limited to last four digits and expiry); 404 `not_found` for an unknown ID, a malformed ID, or another merchant's payment. Integration tests assert that no submit or retrieve response contains the full card number or CVV. Docs: README API section for retrieval.
- **Extra checks:** none
- **Commit:** `feat: retrieve a payment by ID`

### S04: Reject invalid payments
- **Status:** `NOT STARTED`
- **Covers:** FR-004, BR-008, BR-011 (invalid part), BR-012; AC-003, AC-017
- **Depends on:** S02
- **Branch:** `feat/payment-authorization-s04-reject-invalid`
- **Scope:** Validate `POST /payments` per BR-008 and BR-012 and report every failure together as 422 `{"errors": [{"field", "message"}]}`, including missing fields and malformed JSON. A UTC `Clock` bean decides today's date; integration tests replace it to fix today at 2026-11-23. Invalid payments aren't sent to the bank and aren't recorded. Docs: README validation rules and 422 format.
- **Extra checks:** none
- **Commit:** `feat: reject invalid payments`

### S05: Request keys and repeated payments
- **Status:** `NOT STARTED`
- **Covers:** FR-005, FR-006, FR-007, FR-013, BR-001, BR-009, BR-010, BR-014; AC-004, AC-005, AC-006, AC-014, AC-016, AC-019
- **Depends on:** S03, S04
- **Branch:** `feat/payment-authorization-s05-request-keys`
- **Scope:** Per ADR-0003 and ADR-0005: required `Idempotency-Key` header, validated with the body fields (FR-013). Add `request_key` and `fingerprint` (HMAC-SHA256 of card number, expiry, amount, currency, keyed by `gateway.fingerprint-secret`) to `payments`, with a unique constraint on (merchant_id, request_key). Record the payment as `in_progress` before calling the bank (no full card number or CVV stored yet; S07 adds them), then set the bank's result. On a key conflict: `in_progress` gives 409 `in_progress`, a different fingerprint gives 409 `key_reused`, a matching one returns 200 with the existing payment without contacting the bank. Simulated bank digit 6 (authorized after `bank.slow-delay`) for the concurrent-duplicate test: two threads call MockMvc at once with the same key. Docs: README request-key behavior, 409 responses, bank digit 6.
- **Extra checks:** none
- **Commit:** `feat: protect payments against duplicate requests`

### S06: Bank unavailable
- **Status:** `NOT STARTED`
- **Covers:** FR-008, BR-002, BR-011 (unavailable part); AC-007
- **Depends on:** S05
- **Branch:** `feat/payment-authorization-s06-bank-unavailable`
- **Scope:** Simulated bank digit 2 per ADR-0004 (unavailable the first time it sees the card number, remembered in memory, then authorized), signaled as "request never reached the bank". The gateway deletes the `in_progress` payment and returns 503 `bank_unavailable`; a retry with the same key reaches the bank and is charged once. Docs: README 503 response and bank digit 2.
- **Extra checks:** none
- **Commit:** `feat: report an unavailable bank as a retryable failure`

### S07: Pending payments and automatic retries
- **Status:** `NOT STARTED`
- **Covers:** FR-009, FR-010 (without the retry limit), FR-012, BR-001, BR-002, BR-006 (definite-answer part), BR-013, NFR-002, NFR-003; AC-008, AC-009, AC-010, AC-015, AC-018 (definite-answer part)
- **Depends on:** S06
- **Branch:** `feat/payment-authorization-s07-pending-retries`
- **Scope:** Per ADR-0004 and ADR-0005: simulated bank digits 3 (charges, reply lost on the first attempt) and 4 (first attempt never received), signaled as "outcome unknown". Store the full card number and CVV when the payment is recorded and clear them on a definite answer. An unknown outcome sets `pending` and returns 202; a repeat with the same key returns 200 `pending`. A `@Scheduled` fixed-delay job re-sends due pending payments with the same payment ID, counting automatic retries and setting the next attempt time; interval `gateway.retry.interval`, default 5 s (integration tests use 100 ms and Awaitility). At startup, `in_progress` payments become `pending`. The restart test (AC-015) writes pending and in-progress payments into a database file, then starts the application on it. Docs: README pending status, automatic retries, `gateway.retry.interval`, bank digits 3 and 4.
- **Extra checks:** Manual restart smoke test: run the jar with a long `gateway.retry.interval`, submit a card ending in 4, stop the gateway, start it again and see the payment become `authorized`.
- **Commit:** `feat: retry pending payments automatically`

### S08: Unresolved after 10 retries
- **Status:** `NOT STARTED`
- **Covers:** FR-011, FR-010 (retry limit), BR-002, BR-006, BR-013; AC-011, AC-018
- **Depends on:** S07
- **Branch:** `feat/payment-authorization-s08-unresolved`
- **Scope:** Simulated bank digit 5 (never answers). After the 10th automatic retry without a definite answer (11 bank requests in all), the payment becomes `unresolved`, its full card number and CVV are cleared, and no further retries are sent; a repeat with the same key returns 200 `unresolved`, never `declined`. Docs: README unresolved status and bank digit 5.
- **Extra checks:** none
- **Commit:** `feat: mark payments unresolved after 10 automatic retries`

## Risks
- SQLite with one connection: a bank call made inside a transaction or while holding the connection would block every other request (ADR-0002). S05's concurrency test catches it.
- Timing-based tests (slow bank, retries) can be flaky. Tests use short configured intervals and generous Awaitility timeouts.
- MockMvc runs no servlet container or `/error` page: every error body must come from the `@RestControllerAdvice`, and the real HTTP path is checked only by the S01 and S02 extra checks.
- Gradle isn't installed: S01 takes the wrapper files from a Spring Initializr project.
- S05 and S07 are the largest tasks and may take longer than ten minutes to review.
- The simulated bank remembers "unavailable once" only in memory, so a card ending in 2 fails once more after a restart (simulator only).
- `in_progress` is a status the spec doesn't name; it reuses FR-007's "in progress" wording for a payment retrieved while its first bank call is under way.
- The old branch `docs/plan-payment-authorization` from PR #2 (merged) still exists locally but no longer on `origin`, and the old local branch `feat/payment-authorization` holds an unmerged setup commit from the removed plan, which this plan supersedes.
