# Implementation Plan: Championship Standings Endpoints

**Branch**: `007-championship-standings` | **Date**: 2026-03-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/007-championship-standings/spec.md`

## Summary

Add two new JWT-protected endpoints for F1 championship standings: driver standings and constructor standings. All data is fetched from the Jolpica F1 API, cached with 1-hour TTL for the current season and forever for past seasons, and served with stale-cache fallback when Jolpica is unavailable. Implementation follows the established hexagonal architecture patterns (domain models, ports, use cases, adapters, Koin DI modules) already proven in the drivers/teams/circuits/schedule/races features. This is the final Jolpica integration, completing the full F1 data catalog.

## Technical Context

**Language/Version**: Kotlin 2.3.0, JVM 21
**Primary Dependencies**: Ktor 3.4.1 (server, client, auth-jwt, content-negotiation, status-pages), Koin 4.2.0, kotlinx.serialization
**Storage**: In-memory caches (ConcurrentHashMap) — no database for this feature
**Testing**: Kotest 6.1.5 + ktor-server-test-host + ktor-client-mock
**Target Platform**: JVM server (Docker/Linux)
**Project Type**: Web service (REST API)
**Performance Goals**: Correctness and simplicity over performance (per constitution). Caching reduces Jolpica load.
**Constraints**: Must follow existing Clean Architecture layering, Koin DI, stale-cache-with-Warning-header pattern
**Scale/Scope**: 2 new endpoints, ~12 new files, 1 new Koin module, JolpicaClient extended with 2 new methods

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture | PASS | domain models → ports → use cases → adapters → infrastructure. No layer violations. |
| II. API-First Design | PASS | Contracts defined in spec with JSON schemas. REST /v1/ convention. OpenAPI via Ktor plugin. |
| III. Test Coverage | PASS | Integration tests planned for both endpoints (happy path, auth, cache, stale fallback, validation). Kotest + ktor-server-test-host. Fake data sources (not mocks). |
| IV. Security & Input Validation | PASS | Both endpoints behind JWT authenticate block. Season validation in adapter layer before use cases. |
| V. Simplicity & Established Libraries | PASS | Reuses existing patterns (GetDrivers, SeasonCache, CacheEntry). No new dependencies. No speculative abstractions. |
| VI. Dependency Verification via Context7 | PASS | No new dependencies to add — all libraries already in libs.versions.toml. |
| VII. Dependency Injection via Koin | PASS | StandingsModule in infrastructure/di/. Single scope for use cases. Interface binding for caches. Registered in Application.kt. |
| VIII. Static Analysis & Code Style | PASS | ktlintCheck + detekt must pass. No suppressions anticipated. |

**Gate result: PASS — no violations.**

## Project Structure

### Documentation (this feature)

```text
specs/007-championship-standings/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── standings-endpoints.md
└── tasks.md
```

### Source Code (repository root)

```text
src/main/kotlin/com/blaizmiko/f1backend/
├── domain/
│   ├── model/
│   │   ├── DriverStanding.kt          # NEW — driver standing domain model
│   │   ├── ConstructorStanding.kt     # NEW — constructor standing domain model
│   │   └── (existing: CacheEntry.kt, SeasonCache.kt, Constants.kt, Exceptions.kt, ...)
│   └── port/
│       └── StandingsDataSource.kt     # NEW — port for driver & constructor standings
├── usecase/
│   ├── GetDriverStandings.kt          # NEW — driver standings use case
│   └── GetConstructorStandings.kt     # NEW — constructor standings use case
├── adapter/
│   ├── dto/
│   │   └── StandingsResponses.kt      # NEW — standings DTOs
│   ├── port/
│   │   ├── DriverStandingsCache.kt    # NEW — cache interface for driver standings
│   │   └── ConstructorStandingsCache.kt # NEW — cache interface for constructor standings
│   └── route/
│       └── StandingsRoutes.kt         # NEW — /standings/drivers, /standings/constructors
├── infrastructure/
│   ├── cache/
│   │   ├── InMemoryDriverStandingsCache.kt    # NEW
│   │   └── InMemoryConstructorStandingsCache.kt # NEW
│   ├── di/
│   │   └── StandingsModule.kt         # NEW
│   └── external/
│       ├── client/
│       │   └── JolpicaStandingsClient.kt  # NEW — implements StandingsDataSource
│       ├── JolpicaModels.kt           # MODIFIED — add standings response models
│       └── JolpicaMappers.kt          # MODIFIED — add standings toDomain() mappers

src/test/kotlin/com/blaizmiko/f1backend/integration/
├── DriverStandingsEndpointTest.kt     # NEW
└── ConstructorStandingsEndpointTest.kt # NEW
```

**Structure Decision**: Follows the existing single-project hexagonal architecture. Two separate cache interfaces (DriverStandingsCache, ConstructorStandingsCache) rather than a generic keyed cache, because standings are keyed only by season (same pattern as DriverCache/TeamCache) — simpler and more type-safe than a generic approach. One Koin module (StandingsModule) groups both standings use cases since they are logically related. Two separate use cases to keep each independently testable.
