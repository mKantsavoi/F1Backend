# Implementation Plan: Schedule, Race Results & Qualifying Endpoints

**Branch**: `006-schedule-race-results` | **Date**: 2026-03-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/006-schedule-race-results/spec.md`

## Summary

Add five new JWT-protected endpoints for F1 race weekend data: season schedule, next race, race results, qualifying results, and sprint results. All data is fetched from the Jolpica F1 API, cached with endpoint-specific TTLs (6h schedule, 1h next race, 5min/forever results by season, forever qualifying/sprint), and served with stale-cache fallback when Jolpica is unavailable. Implementation follows the established hexagonal architecture patterns (domain models, ports, use cases, adapters, Koin DI modules) already proven in the drivers/teams/circuits features.

## Technical Context

**Language/Version**: Kotlin 2.3.0, JVM 21
**Primary Dependencies**: Ktor 3.4.1 (server, client, auth-jwt, content-negotiation, status-pages), Koin 4.2.0, kotlinx.serialization
**Storage**: In-memory caches (ConcurrentHashMap, AtomicReference) — no database for this feature
**Testing**: Kotest 6.1.5 + ktor-server-test-host + ktor-client-mock
**Target Platform**: JVM server (Docker/Linux)
**Project Type**: Web service (REST API)
**Performance Goals**: Correctness and simplicity over performance (per constitution). Caching reduces Jolpica load.
**Constraints**: Must follow existing Clean Architecture layering, Koin DI, stale-cache-with-Warning-header pattern
**Scale/Scope**: 5 new endpoints, ~15 new files, 2 new Koin modules, JolpicaClient extended with 5 new methods

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture | PASS | domain models → ports → use cases → adapters → infrastructure. No layer violations. |
| II. API-First Design | PASS | Contracts defined in spec with JSON schemas. REST /v1/ convention. OpenAPI via Ktor plugin. |
| III. Test Coverage | PASS | Integration tests planned for all 5 endpoints (happy path, auth, cache, stale fallback, validation). Kotest + ktor-server-test-host. Fake data sources (not mocks). |
| IV. Security & Input Validation | PASS | All endpoints behind JWT authenticate block. Season/round validation in adapter layer before use cases. |
| V. Simplicity & Established Libraries | PASS | Reuses existing patterns (GetDrivers, SeasonCache, CacheEntry). No new dependencies. No speculative abstractions. |
| VI. Dependency Verification via Context7 | PASS | No new dependencies to add — all libraries already in libs.versions.toml. |
| VII. Dependency Injection via Koin | PASS | ScheduleModule + RacesModule in infrastructure/di/. Single scope for use cases. Interface binding for caches. Registered in Application.kt. |
| VIII. Static Analysis & Code Style | PASS | ktlintCheck + detekt must pass. No suppressions anticipated. |

**Gate result: PASS — no violations.**

## Project Structure

### Documentation (this feature)

```text
specs/006-schedule-race-results/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── schedule-endpoints.md
│   └── races-endpoints.md
└── tasks.md
```

### Source Code (repository root)

```text
src/main/kotlin/com/blaizmiko/f1backend/
├── domain/
│   ├── model/
│   │   ├── RaceWeekend.kt          # NEW — schedule domain model
│   │   ├── RaceResult.kt           # NEW — race/sprint result domain model
│   │   ├── QualifyingResult.kt     # NEW — qualifying result domain model
│   │   └── (existing: CacheEntry.kt, SeasonCache.kt, Constants.kt, Exceptions.kt, ...)
│   └── port/
│       ├── ScheduleDataSource.kt   # NEW — port for schedule + next race
│       └── RaceDataSource.kt       # NEW — port for results, qualifying, sprint
├── usecase/
│   ├── GetSchedule.kt              # NEW — season schedule use case
│   ├── GetNextRace.kt              # NEW — next race use case
│   ├── GetRaceResults.kt           # NEW — race results use case
│   ├── GetQualifyingResults.kt     # NEW — qualifying results use case
│   └── GetSprintResults.kt         # NEW — sprint results use case
├── adapter/
│   ├── dto/
│   │   ├── ScheduleResponses.kt    # NEW — schedule/next-race DTOs
│   │   └── RaceResponses.kt        # NEW — results/qualifying/sprint DTOs
│   ├── port/
│   │   ├── ScheduleCache.kt        # NEW — cache interface for schedule data
│   │   ├── NextRaceCache.kt        # NEW — cache interface for next race
│   │   └── RaceResultCache.kt      # NEW — generic cache for results/qualifying/sprint
│   └── route/
│       ├── ScheduleRoutes.kt       # NEW — /schedule, /schedule/next
│       ├── RaceRoutes.kt           # NEW — /races/{s}/{r}/results|qualifying|sprint
│       └── (existing: RouteValidation.kt — extended with validateRound)
├── infrastructure/
│   ├── cache/
│   │   ├── InMemoryScheduleCache.kt    # NEW
│   │   ├── InMemoryNextRaceCache.kt    # NEW
│   │   └── InMemoryRaceResultCache.kt  # NEW — keyed by "type:season:round"
│   ├── di/
│   │   ├── ScheduleModule.kt           # NEW
│   │   └── RacesModule.kt              # NEW
│   └── external/
│       ├── JolpicaClient.kt            # MODIFIED — add 5 new fetch methods
│       └── JolpicaModels.kt            # MODIFIED — add response models for schedule/results/qualifying/sprint

src/test/kotlin/com/blaizmiko/f1backend/integration/
├── ScheduleEndpointTest.kt         # NEW
├── NextRaceEndpointTest.kt         # NEW
├── RaceResultsEndpointTest.kt      # NEW
├── QualifyingEndpointTest.kt       # NEW
└── SprintEndpointTest.kt           # NEW
```

**Structure Decision**: Follows the existing single-project hexagonal architecture. New files mirror the established pattern (one use case per endpoint, cache interface in adapter/port, implementation in infrastructure/cache, Koin module per feature domain). Two feature modules (Schedule, Races) group logically related endpoints.
