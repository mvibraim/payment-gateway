# ADR-0001: Java, Spring Boot and the Gradle wrapper, with separate unit and integration suites

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
The repository has no code, and the spec (`docs/specs/payment-authorization.md`) imposes no technology. The gateway is a JSON HTTP API (ADR-0003) with a database (ADR-0002) and a scheduled retry job (ADR-0005). Every task must run a unit suite, an integration suite and a build check. Java 25 is installed on the development machine; Maven and Gradle aren't.

## Decision
Chose Java 25, Spring Boot and the Gradle wrapper with the Kotlin DSL (`build.gradle.kts`) over TypeScript with Fastify and Python with FastAPI, because Spring covers HTTP, JDBC, scheduling and testing without extra libraries, and the engineer chose the Gradle wrapper over the Maven wrapper.

- Unit tests: JUnit 5 in `src/test/java`, without a Spring context. Run with `./gradlew test`.
- Integration tests: a separate `integrationTest` suite (Gradle JVM Test Suite plugin) in `src/integrationTest/java`, which `check` runs. Each test class starts the full application with `@SpringBootTest` and `@AutoConfigureMockMvc` (no network port) against a fresh SQLite file in a JUnit `@TempDir`, set through `@DynamicPropertySource`. Requests go through `MockMvc`, asserted with `jsonPath` and AssertJ; asynchronous status changes are awaited with Awaitility; tests that depend on today's date replace the UTC `Clock` bean. Run with `./gradlew integrationTest`.
- Build check: `./gradlew build` compiles with `-Xlint:all -Werror`, runs both suites and builds the executable jar with `bootJar`.
- On Windows PowerShell, use `.\gradlew.bat` instead of `./gradlew`.

## Consequences
One toolchain covers the whole gateway, and the wrapper needs no Gradle install. Gradle isn't installed, so the setup task takes the wrapper files from a Spring Initializr project (Gradle, Kotlin DSL) instead of running `gradle wrapper`. MockMvc doesn't run a servlet container or Spring Boot's `/error` page, so every error body must come from a `@RestControllerAdvice`, and the real HTTP path is checked by starting the jar. Revisit if the gateway must run where a JVM isn't available.
