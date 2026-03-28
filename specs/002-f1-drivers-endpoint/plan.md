# Implementation Plan: F1 Drivers Endpoint

**Branch**: `002-f1-drivers-endpoint` | **Date**: 2026-03-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-f1-drivers-endpoint/spec.md`

## Summary

Expose a JWT-protected `GET /api/v1/drivers` endpoint that fetches F1 driver data from the Jolpica API, transforms it into our domain model, caches it per-season in memory (24h TTL), and gracefully degrades when the external API is unavailable. This is the first external API integration and establishes the reusable pattern (HTTP client, caching, retry, rate limiting) for all future Jolpica endpoints.

## Technical Context

**Language/Version**: Kotlin 2.3.0, JVM 21
**Primary Dependencies**: Ktor 3.4.1 (server + client CIO engine), kotlinx.serialization, Ktor HttpRequestRetry plugin, Ktor HttpTimeout plugin
**Storage**: In-memory `ConcurrentHashMap` for caching (no database for this feature)
**Testing**: Kotest 6.1.5 + ktor-server-test-host + ktor-client-mock
**Target Platform**: Linux server (Docker container)
**Project Type**: Web service (REST API)
**Performance Goals**: Cached responses < 50ms; external fetch < 5s timeout
**Constraints**: Jolpica rate limits: 4 req/s, 500 req/hr; 24h cache TTL per season
**Scale/Scope**: Single endpoint, ~20-25 drivers per response, low memory footprint

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture | PASS | Domain: Driver entity + cache port. Use-cases: GetDrivers. Adapters: route + DTOs. Infrastructure: JolpicaClient + InMemoryDriverCache. |
| II. API-First Design | PASS | Contract defined in `contracts/` before implementation. REST `/api/v1/drivers`. OpenAPI/Swagger annotations required. |
| III. Test Coverage | PASS | Integration test with mocked Jolpica client via ktor-client-mock. No DB needed so testcontainers not required for this feature. |
| IV. Security & Input Validation | PASS | JWT-protected endpoint. Season query parameter validated in adapter layer. |
| V. Simplicity & Established Libraries | PASS | Using Ktor HTTP client (already in project ecosystem). ConcurrentHashMap for cache (stdlib). No new frameworks. |
| VI. Dependency Verification via Context7 | PASS | Ktor client CIO, HttpRequestRetry, HttpTimeout verified via Context7. ktor-client-mock for testing. |

**Post-Phase 1 Re-check**: All principles remain satisfied. The design uses interface-based ports for the external client and cache, keeping the domain layer free of framework dependencies.

## Project Structure

### Documentation (this feature)

```text
specs/002-f1-drivers-endpoint/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── drivers-endpoint.md
└── tasks.md
```

### Source Code (repository root)

```text
src/main/kotlin/com/blaizmiko/
├── adapter/
│   ├── dto/
│   │   └── DriverResponses.kt          # Response DTOs with @Serializable
│   └── route/
│       └── DriverRoutes.kt             # GET /api/v1/drivers route handler
├── domain/
│   ├── model/
│   │   └── Driver.kt                   # Driver entity (pure Kotlin)
│   └── port/
│       ├── DriverDataSource.kt         # Interface for external driver data
│       └── DriverCache.kt              # Interface for cache operations
├── infrastructure/
│   ├── external/
│   │   ├── JolpicaClient.kt            # Ktor HTTP client for Jolpica API
│   │   └── JolpicaModels.kt            # Jolpica JSON response models
│   └── cache/
│       └── InMemoryDriverCache.kt      # ConcurrentHashMap-based cache
└── usecase/
    └── GetDrivers.kt                   # Orchestrates cache check → fetch → transform

src/test/kotlin/com/blaizmiko/
└── integration/
    └── DriversEndpointTest.kt          # Integration test with mocked Jolpica
```

**Structure Decision**: Follows existing Clean Architecture layout from 001-jwt-auth. New `domain/port/` package for interfaces (replaces `domain/repository/` pattern for non-DB dependencies). New `infrastructure/external/` package for third-party API clients. New `infrastructure/cache/` package for caching implementations.

## Complexity Tracking

No constitution violations. No complexity justifications needed.
