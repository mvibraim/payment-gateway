# ADR-0001: Language, framework and build tool

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
The gateway is a JSON HTTP API built from scratch; the repository has no code yet. A language, web framework and build tool must be chosen before any task can start. Maven isn't installed on the development machine. Approved by the engineer on 2026-11-23 for the earlier payment-gateway plan and adopted unchanged for payment-authorization.

## Decision
Chose Java 25 with the latest Spring Boot 4.x release (`spring-boot-starter-web`, `spring-boot-starter-jdbc`, `spring-boot-starter-test`), built with the Maven Wrapper (scripts only, no wrapper jar), over Python 3.14 + FastAPI and TypeScript on Node 26 + Fastify, because it was the engineer's choice.

## Consequences
JDK 25 is the only prerequisite; the wrapper downloads Maven on the first build, which needs network access. Revisit if the team standardizes on another stack.
