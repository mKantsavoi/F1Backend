# Implementation Plan: JWT Authentication & Authorization

**Branch**: `001-jwt-auth` | **Date**: 2026-03-26 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-jwt-auth/spec.md`

## Summary

Build a JWT-based authentication and authorization system for the F1 backend API. The system provides user registration, login, token-based access control, refresh token rotation with reuse detection, logout, and profile management. Seven REST endpoints under `/api/v1/auth` using Ktor with Exposed ORM on PostgreSQL, following Clean Architecture layering.

## Technical Context

**Language/Version**: Kotlin 2.3.0, JVM 21
**Primary Dependencies**: Ktor 3.4.1 (server-auth-jwt, content-negotiation, status-pages, openapi, swagger), kotlinx.serialization, at.favre.lib:bcrypt:0.10.2
**Storage**: PostgreSQL via Exposed 1.0.0 (DSL) + HikariCP 6.2.1
**Testing**: kotest 6.1.5 + ktor-server-test-host 3.4.1 + testcontainers 1.21.4
**Target Platform**: Linux server (Docker container), development on JVM 21
**Project Type**: Web service (REST API)
**Performance Goals**: Correctness and simplicity over performance (per constitution). Aspirational: <200ms p95 for auth endpoints.
**Constraints**: No Spring. Ktor only. Clean Architecture layering enforced. Secrets from environment variables only.
**Scale/Scope**: Single-project Gradle build. 7 endpoints, 2 database tables, ~20 source files.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Research Check

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Clean Architecture | PASS | Four-layer package structure: domain → usecase → adapter → infrastructure. Domain has zero framework imports. Repository interfaces in domain, implementations in infrastructure. |
| II. API-First Design | PASS | API contract defined in `contracts/auth-api.md` before implementation. REST with `/v1/` versioning. kotlinx.serialization DTOs. OpenAPI/Swagger plugins included. |
| III. Test Coverage | PASS | Testing stack includes kotest + ktor-server-test-host + testcontainers (real PostgreSQL). Integration test for full auth lifecycle specified in SC-006. |
| IV. Security & Input Validation | PASS | Input validation at adapter layer (email, password, username). BCrypt password hashing. JWT auth on protected endpoints. Secrets from environment. OWASP considerations addressed (no credential hints, token hashing). |
| V. Simplicity & Established Libraries | PASS | Ktor only (no Spring). All dependencies are well-established (Exposed, kotest, bcrypt, testcontainers). No custom crypto. DSL over DAO for Exposed (simpler). No premature abstractions. |
| Technology Stack | PASS | All choices match constitution table exactly: Kotlin, Ktor, Gradle Kotlin DSL, PostgreSQL/Exposed, kotlinx.serialization, kotest, testcontainers, Docker, REST /v1/. |

**Gate result**: PASS — no violations.

### Post-Design Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture | PASS | Domain layer contains only pure Kotlin (User, RefreshToken models + repository interfaces). Use-cases depend only on domain. Adapters handle Ktor routing and DTO mapping. Infrastructure implements repository interfaces with Exposed. |
| II. API-First Design | PASS | Full contract in `contracts/auth-api.md` with request/response schemas, status codes, JWT claims. |
| III. Test Coverage | PASS | Plan includes unit tests for use-cases, route tests with ktor-server-test-host, and integration tests with testcontainers PostgreSQL. |
| IV. Security & Input Validation | PASS | Validation in adapter layer. BCrypt for passwords, SHA-256 for token hashing, JWT secrets from env. Generic error on login failure. |
| V. Simplicity & Established Libraries | PASS | No unnecessary abstractions. Direct use of Exposed DSL. Standard Ktor auth plugin. |

**Gate result**: PASS — no violations.

## Project Structure

### Documentation (this feature)

```text
specs/001-jwt-auth/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: technology decisions
├── data-model.md        # Phase 1: entity definitions
├── quickstart.md        # Phase 1: setup instructions
├── contracts/
│   └── auth-api.md      # Phase 1: REST API contract
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
src/main/kotlin/com/blaizmiko/
├── domain/
│   ├── model/
│   │   ├── User.kt                        # User entity (pure Kotlin)
│   │   └── RefreshToken.kt                # RefreshToken entity (pure Kotlin)
│   └── repository/
│       ├── UserRepository.kt              # Interface: find/create/update users
│       └── RefreshTokenRepository.kt      # Interface: find/create/revoke tokens
├── usecase/
│   ├── RegisterUser.kt                    # Create account + issue tokens
│   ├── LoginUser.kt                       # Verify credentials + issue tokens
│   ├── RefreshTokens.kt                   # Rotate tokens + reuse detection
│   ├── LogoutUser.kt                      # Revoke refresh token
│   ├── GetProfile.kt                      # Return user info
│   ├── UpdateProfile.kt                   # Change username
│   └── ChangePassword.kt                  # Change password with confirmation
├── adapter/
│   ├── route/
│   │   └── AuthRoutes.kt                  # Ktor route definitions for /api/v1/auth
│   └── dto/
│       ├── AuthRequests.kt                # Request DTOs (kotlinx.serialization)
│       └── AuthResponses.kt              # Response DTOs (kotlinx.serialization)
├── infrastructure/
│   ├── persistence/
│   │   ├── DatabaseFactory.kt             # HikariCP + Exposed setup
│   │   ├── table/
│   │   │   ├── UsersTable.kt              # Exposed table definition
│   │   │   └── RefreshTokensTable.kt      # Exposed table definition
│   │   └── repository/
│   │       ├── ExposedUserRepository.kt   # UserRepository implementation
│   │       └── ExposedRefreshTokenRepository.kt  # RefreshTokenRepository implementation
│   ├── security/
│   │   ├── JwtProvider.kt                 # JWT generation + validation config
│   │   ├── PasswordHasher.kt              # BCrypt wrapper
│   │   └── TokenHasher.kt                # SHA-256 for refresh tokens
│   └── config/
│       └── AppConfig.kt                   # Environment-based configuration
├── Application.kt                         # Entry point (existing)
└── Routing.kt                             # Module configuration (existing)

src/main/resources/
├── application.yaml                       # Ktor + DB + JWT config (existing, to be extended)
└── logback.xml                            # Logging config (existing)

src/test/kotlin/com/blaizmiko/
├── usecase/                               # Unit tests for business logic
│   ├── RegisterUserTest.kt
│   ├── LoginUserTest.kt
│   └── RefreshTokensTest.kt
├── adapter/
│   └── route/
│       └── AuthRoutesTest.kt             # Route tests with ktor-server-test-host
└── integration/
    └── AuthLifecycleTest.kt              # Full lifecycle: register → access → refresh → logout

docker-compose.yml                         # PostgreSQL + app services
Dockerfile                                 # Multi-stage build for the application
```

**Structure Decision**: Single-project layout following Clean Architecture with four packages (domain, usecase, adapter, infrastructure). This is a backend-only API service — no frontend. The existing `Application.kt` and `Routing.kt` are extended in place.

## Complexity Tracking

No constitution violations to justify. All design choices align with mandated stack and principles.
