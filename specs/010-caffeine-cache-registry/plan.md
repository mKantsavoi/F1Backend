# Implementation Plan: Caffeine Cache Registry

**Branch**: `010-caffeine-cache-registry` | **Date**: 2026-03-28 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/010-caffeine-cache-registry/spec.md`

## Summary

Replace all custom in-memory caching (6 cache interfaces, 6 InMemory implementations, manual TTL logic in 8+ use cases) with Caffeine + Aedile (Kotlin coroutines wrapper). Centralize all cache configuration in a `CacheSpec` enum and `CacheRegistry` class. Preserve all existing behavior: same TTLs, stale-on-error fallback, thundering herd prevention, retry throttling.

## Technical Context

**Language/Version**: Kotlin 2.3.0, JVM 21
**Primary Dependencies**: Ktor 3.4.1, Koin 4.2.0, Caffeine 3.2.3 (new), Aedile 3.0.2 (new)
**Storage**: PostgreSQL via Exposed ORM (unchanged), Caffeine in-memory caches (new)
**Testing**: kotest 6.1.5, ktor-server-test-host, testcontainers
**Target Platform**: JVM 21 server (Netty)
**Project Type**: Web service (REST API)
**Performance Goals**: Preserve existing cache hit rates; eliminate thread-blocking in cache loaders
**Constraints**: Pure internal refactoring — zero external behavior changes
**Scale/Scope**: 14 cache categories, 9 use cases to refactor, ~12 files to delete, ~3 files to create

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture | PASS | CacheSpec/CacheRegistry in infrastructure layer. Use cases depend on CacheProvider port interface (defined in adapter/port/). CacheRegistry implements CacheProvider. Routes remain in adapter layer. |
| II. API-First Design | PASS | No new endpoints. Existing endpoints unchanged. |
| III. Test Coverage | PASS | Existing tests must pass. New tests for CacheRegistry and admin endpoint required. |
| IV. Security & Input Validation | PASS | Admin endpoint protected by existing JWT auth. No new external input surfaces. |
| V. Simplicity & Established Libraries | PASS | Caffeine is the most established JVM cache library. Aedile is a thin Kotlin wrapper. Replaces 12 custom files with 2 library dependencies. |
| VI. Dependency Verification via Context7 | PASS | Caffeine verified via Context7 (v3.2.3). Aedile not in Context7; verified via GitHub releases (v3.0.2, Dec 2025). |
| VII. Dependency Injection via Koin | PASS | CacheRegistry as singleton in dedicated CacheModule, bound to CacheProvider interface via Koin's `bind` operator. Use cases inject CacheProvider. One module per feature preserved. |
| VIII. Static Analysis & Code Style | PASS | All code must pass ktlintCheck and detekt. |

**Post-Phase 1 Re-check**: All gates remain PASS. No architectural violations introduced.

## Project Structure

### Documentation (this feature)

```text
specs/010-caffeine-cache-registry/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: dependency research
├── data-model.md        # Phase 1: entity definitions
├── quickstart.md        # Phase 1: setup guide
├── contracts/           # Phase 1 (none for this feature)
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
src/main/kotlin/com/blaizmiko/f1backend/
├── adapter/
│   ├── port/
│   │   ├── CacheProvider.kt               # NEW: port interface for CacheRegistry
│   │   ├── CircuitCache.kt                # DELETE
│   │   ├── ScheduleCache.kt               # DELETE
│   │   ├── DriverStandingsCache.kt        # DELETE
│   │   ├── ConstructorStandingsCache.kt   # DELETE
│   │   ├── NextRaceCache.kt               # DELETE
│   │   └── RaceResultCache.kt             # DELETE
├── domain/
│   └── model/
│       ├── CacheEntry.kt                  # DELETE
│       └── SeasonCache.kt                 # KEEP (used for response building)
├── infrastructure/
│   ├── cache/
│   │   ├── CacheSpec.kt                   # NEW: enum with all cache configs
│   │   ├── CacheRegistry.kt              # NEW: centralized cache manager
│   │   ├── InMemoryCircuitCache.kt        # DELETE
│   │   ├── InMemoryScheduleCache.kt       # DELETE
│   │   ├── InMemoryDriverStandingsCache.kt    # DELETE
│   │   ├── InMemoryConstructorStandingsCache.kt # DELETE
│   │   ├── InMemoryNextRaceCache.kt       # DELETE
│   │   └── InMemoryRaceResultCache.kt     # DELETE
│   └── di/
│       ├── CacheModule.kt                 # NEW: Koin module for CacheRegistry
│       ├── CircuitsModule.kt              # MODIFY: remove cache binding
│       ├── ScheduleModule.kt              # MODIFY: remove cache bindings
│       ├── RacesModule.kt                 # MODIFY: remove cache binding
│       └── StandingsModule.kt             # MODIFY: remove cache bindings
├── usecase/
│   ├── GetCircuits.kt                     # MODIFY: simplify with CacheRegistry
│   ├── GetSchedule.kt                     # MODIFY: simplify with CacheRegistry
│   ├── GetNextRace.kt                     # MODIFY: simplify with CacheRegistry
│   ├── GetRaceResults.kt                  # MODIFY: simplify with CacheRegistry
│   ├── GetQualifyingResults.kt            # MODIFY: simplify with CacheRegistry
│   ├── GetSprintResults.kt                # MODIFY: simplify with CacheRegistry
│   ├── GetDriverStandings.kt              # MODIFY: simplify with CacheRegistry
│   ├── GetConstructorStandings.kt         # MODIFY: simplify with CacheRegistry
│   └── GetPersonalizedFeed.kt            # MODIFY: replace internal ConcurrentHashMap with CacheRegistry
└── Application.kt                         # MODIFY: add cacheModule

gradle/libs.versions.toml                  # MODIFY: add caffeine + aedile versions
build.gradle.kts                           # MODIFY: add caffeine + aedile dependencies
```

**Structure Decision**: Follows existing project structure. New files placed in established directories per Clean Architecture layers. CacheSpec and CacheRegistry in `infrastructure/cache/` alongside (soon-deleted) InMemory implementations.

## Complexity Tracking

No constitution violations. No complexity justifications needed.
