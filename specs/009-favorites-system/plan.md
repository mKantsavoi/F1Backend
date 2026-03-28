# Implementation Plan: Favorites System

**Branch**: `009-favorites-system` | **Date**: 2026-03-28 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/009-favorites-system/spec.md`

## Summary

Build a favorites system allowing authenticated users to save favorite drivers and teams, view them as full entity cards, check individual favorite status, and consume a personalized feed aggregating championship standings and last race results. Implementation adds two junction tables, a new repository layer, 9 use cases, a Koin module, and a route handler with 9 endpoints -- all following existing Clean Architecture patterns.

## Technical Context

**Language/Version**: Kotlin 2.3.0, JVM 21
**Primary Dependencies**: Ktor 3.4.1 (server-auth-jwt, content-negotiation, status-pages), kotlinx.serialization, Koin, Exposed ORM
**Storage**: PostgreSQL via Exposed ORM (two new junction tables: `favorite_drivers`, `favorite_teams`)
**Testing**: Kotest (StringSpec) + ktor-server-test-host + testcontainers (PostgreSQL)
**Target Platform**: JVM 21 Linux/Windows server
**Project Type**: REST web-service
**Performance Goals**: Standard CRUD latency (<200ms p95). Feed endpoint uses 30s per-user cache.
**Constraints**: No new dependencies required. All existing endpoints unchanged. ktlint + detekt must pass.
**Scale/Scope**: ~20 drivers, ~10 teams max per user (F1 grid constraint). Feed aggregates from existing in-memory caches.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture | PASS | Domain entities + repository interfaces in domain layer; Exposed implementations in infrastructure; routes in adapter; use cases depend only on domain interfaces |
| II. API-First Design | PASS | 9 REST endpoints under `/api/v1/favorites/`, OpenAPI/Swagger annotations required, kotlinx.serialization DTOs |
| III. Test Coverage | PASS | Integration tests with testcontainers PostgreSQL, kotest StringSpec, covering all 9 endpoints + edge cases |
| IV. Security & Input Validation | PASS | All endpoints behind JWT `authenticate` block; driver/team existence validated before insert; parameterized Exposed queries |
| V. Simplicity | PASS | No new dependencies; reuses existing cache interfaces and repositories; no speculative abstractions |
| VI. Context7 Verification | PASS | No new dependencies to verify |
| VII. Koin DI | PASS | `favoritesModule` with single-scope use cases, interface binding for repositories |
| VIII. Static Analysis | PASS | ktlintCheck + detekt must pass before merge |

No violations. All gates clear.

## Project Structure

### Documentation (this feature)

```text
specs/009-favorites-system/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (API contracts)
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
src/main/kotlin/com/blaizmiko/f1backend/
├── adapter/
│   ├── dto/
│   │   └── FavoritesResponses.kt          # New: all favorites DTOs
│   └── route/
│       └── FavoritesRoutes.kt             # New: 9 endpoint handlers
├── domain/
│   └── repository/
│       └── FavoriteRepository.kt          # New: interface for both driver + team favorites
├── infrastructure/
│   ├── di/
│   │   └── FavoritesModule.kt             # New: Koin module
│   └── persistence/
│       ├── table/
│       │   ├── FavoriteDriversTable.kt    # New: Exposed DSL table
│       │   └── FavoriteTeamsTable.kt      # New: Exposed DSL table
│       └── repository/
│           └── ExposedFavoriteRepository.kt  # New: Exposed implementation
├── usecase/
│   ├── AddFavoriteDriver.kt               # New
│   ├── RemoveFavoriteDriver.kt            # New
│   ├── GetFavoriteDrivers.kt              # New
│   ├── CheckDriverFavorite.kt             # New
│   ├── AddFavoriteTeam.kt                 # New
│   ├── RemoveFavoriteTeam.kt              # New
│   ├── GetFavoriteTeams.kt                # New
│   ├── CheckTeamFavorite.kt               # New
│   ├── GetPersonalizedFeed.kt             # New
│   └── [existing use cases unchanged]
├── Application.kt                         # Modified: add favoritesModule
└── Routing.kt                             # Modified: add favoritesRoutes()

src/test/kotlin/com/blaizmiko/f1backend/
└── integration/
    └── FavoritesEndpointTest.kt           # New: comprehensive integration tests
```

**Structure Decision**: Follows the established single-project Clean Architecture layout. New files mirror existing patterns (e.g., `DriversModule` → `FavoritesModule`, `ExposedDriverRepository` → `ExposedFavoriteRepository`).

## Complexity Tracking

No violations to justify. All design decisions follow established patterns.
