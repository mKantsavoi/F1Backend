# Implementation Plan: Teams & Circuits Data Endpoints

**Branch**: `005-teams-circuits-endpoints` | **Date**: 2026-03-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/005-teams-circuits-endpoints/spec.md`

## Summary

Add two new protected endpoints (GET /api/v1/teams and GET /api/v1/circuits) that fetch F1 constructor and circuit data from the Jolpica API, cache it in memory, and serve it to authenticated users. Both endpoints replicate the exact architecture established by the drivers endpoint: domain model, port interfaces, use case with mutex-guarded caching and stale fallback, JolpicaClient data source methods, Koin DI module, adapter route with season validation (teams only), and integration tests.

## Technical Context

**Language/Version**: Kotlin 2.3.0, JVM 21
**Primary Dependencies**: Ktor 3.4.1 (server + client), kotlinx.serialization, Koin 4.2.0, ktor-client-cio with HttpRequestRetry
**Storage**: In-memory ConcurrentHashMap caches (no database for this feature)
**Testing**: Kotest 6.1.5 + ktor-server-test-host, fake data sources (no testcontainers needed — no DB interaction)
**Target Platform**: Linux server (Docker)
**Project Type**: Web service (REST API)
**Performance Goals**: Inherited from drivers — cache-first serving, 60s retry throttle
**Constraints**: Follow existing drivers pattern exactly; no new dependencies
**Scale/Scope**: ~20 teams per season, ~80 circuits total — all fit in single responses

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture | PASS | Domain models + ports in domain, use cases in usecase layer, routes in adapter, JolpicaClient + cache in infrastructure. Same layering as drivers. |
| II. API-First Design | PASS | Contracts defined in spec with exact JSON shapes. Routes under /api/v1/ with REST conventions. OpenAPI/Swagger auto-generated via Ktor plugin. |
| III. Test Coverage | PASS | Integration tests planned for both endpoints covering: happy path, auth check, cache hit, stale fallback, no-cache failure, season validation (teams). |
| IV. Security & Input Validation | PASS | JWT required on both endpoints. Season parameter validated in adapter layer before reaching use case (same as drivers). |
| V. Simplicity & Established Libraries | PASS | No new dependencies. Reuses existing JolpicaClient, CacheEntry, Koin patterns. No new abstractions — just new instances of established patterns. |
| VI. Dependency Verification | PASS | No new dependencies to verify. All libraries already in use. |

## Project Structure

### Documentation (this feature)

```text
specs/005-teams-circuits-endpoints/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── teams.md
│   └── circuits.md
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
src/main/kotlin/com/blaizmiko/f1backend/
├── Application.kt                          # Add teamsModule, circuitsModule to Koin install
├── Routing.kt                              # Add teamRoutes(), circuitRoutes() inside authenticate block
├── adapter/
│   ├── dto/
│   │   ├── TeamResponses.kt                # NEW: TeamsResponse, TeamDto
│   │   └── CircuitResponses.kt             # NEW: CircuitsResponse, CircuitDto
│   └── route/
│       ├── TeamRoutes.kt                   # NEW: GET /teams with season param
│       └── CircuitRoutes.kt                # NEW: GET /circuits
├── domain/
│   ├── model/
│   │   ├── Team.kt                         # NEW: Team domain entity
│   │   └── Circuit.kt                      # NEW: Circuit domain entity
│   └── port/
│       ├── TeamDataSource.kt               # NEW: Interface for team data fetching
│       ├── TeamCache.kt                    # NEW: Interface for team caching
│       ├── CircuitDataSource.kt            # NEW: Interface for circuit data fetching
│       └── CircuitCache.kt                 # NEW: Interface for circuit caching
├── infrastructure/
│   ├── cache/
│   │   ├── InMemoryTeamCache.kt            # NEW: ConcurrentHashMap cache for teams
│   │   └── InMemoryCircuitCache.kt         # NEW: ConcurrentHashMap cache for circuits
│   ├── di/
│   │   ├── ClientModule.kt                 # MODIFY: Bind JolpicaClient to TeamDataSource, CircuitDataSource
│   │   ├── TeamsModule.kt                  # NEW: Koin module for teams
│   │   └── CircuitsModule.kt               # NEW: Koin module for circuits
│   └── external/
│       ├── JolpicaClient.kt                # MODIFY: Add fetchTeams(), fetchCircuits() methods; implement new interfaces
│       └── JolpicaModels.kt                # MODIFY: Add constructor/circuit Jolpica response models
└── usecase/
    ├── GetTeams.kt                         # NEW: Teams use case (same pattern as GetDrivers)
    └── GetCircuits.kt                      # NEW: Circuits use case (adapted for indefinite cache)

src/test/kotlin/com/blaizmiko/f1backend/integration/
├── TeamsEndpointTest.kt                    # NEW: Integration tests for teams
└── CircuitsEndpointTest.kt                 # NEW: Integration tests for circuits
```

**Structure Decision**: Single-project Ktor web service. All new files follow the existing Clean Architecture layout with domain/port/adapter/infrastructure layers. No structural changes — only additions within existing directories.

## Complexity Tracking

No constitution violations. No complexity justifications needed.
