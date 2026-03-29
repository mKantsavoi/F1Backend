# Tasks: Caffeine Cache Registry

**Input**: Design documents from `/specs/010-caffeine-cache-registry/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md

**Tests**: CacheRegistry unit test included per Constitution III (tests MUST accompany every feature). Existing tests must continue to pass.

**Organization**: Tasks are grouped by user story. US2 (Centralized Config) and US3 (Non-Blocking Ops) are foundational — they are achieved by building CacheSpec + CacheRegistry, which is a prerequisite for US1 (endpoint migration).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: Add Caffeine and Aedile dependencies to the project

- [X] T001 Add caffeine (3.2.3) and aedile (3.0.2) versions and library entries to gradle/libs.versions.toml
- [X] T002 Add caffeine and aedile implementation dependencies to build.gradle.kts

**Checkpoint**: Project compiles with new dependencies (`./gradlew build`)

---

## Phase 2: Foundational — Centralized Config + Non-Blocking Caches (US2 + US3)

**Purpose**: Build the CacheSpec enum, CacheRegistry, and Koin module that all use case migrations depend on. This phase delivers User Story 2 (centralized config) and User Story 3 (non-blocking cache operations).

**Independent Test (US2)**: All cache configurations visible in one enum. Changing a value in CacheSpec changes behavior without editing other files.

**Independent Test (US3)**: CacheRegistry creates Aedile caches with suspend loaders. No runBlocking in cache infrastructure code.

- [X] T003 [P] [US2] Create CacheSpec enum with all 14 cache categories in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/cache/CacheSpec.kt
- [X] T004 [P] [US2] Create CacheProvider port interface in src/main/kotlin/com/blaizmiko/f1backend/adapter/port/CacheProvider.kt
- [X] T005 [US3] Create CacheRegistry class implementing CacheProvider with typed cache creation, fallback maps, and stats access in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/cache/CacheRegistry.kt
- [X] T006 Create CacheModule Koin module registering CacheRegistry as singleton bound to CacheProvider interface in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/CacheModule.kt
- [X] T007 Register cacheModule in Application.kt Koin modules list in src/main/kotlin/com/blaizmiko/f1backend/Application.kt
- [X] T008 Write CacheRegistry unit test covering cache creation, TTL expiration, eviction, and stale-on-error fallback in src/test/kotlin/com/blaizmiko/f1backend/usecase/CacheRegistryTest.kt

**Checkpoint**: CacheRegistry is injectable via Koin. Aedile caches can be created for any CacheSpec. Unit test passes. Project compiles.

---

## Phase 3: User Story 1 — All Endpoints Continue Working Identically (Priority: P1) MVP

**Goal**: Migrate all 9 use cases from custom cache implementations to CacheRegistry. Delete all old cache interfaces and implementations. All endpoints return identical responses.

**Independent Test**: Run full existing test suite. Hit every endpoint before and after migration and compare response bodies, status codes, and Warning headers.

### Implementation for User Story 1

- [X] T009 [US1] Refactor GetCircuits to use CacheProvider instead of CircuitCache in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetCircuits.kt
- [X] T010 [US1] Refactor GetSchedule to use CacheProvider instead of ScheduleCache in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetSchedule.kt
- [X] T011 [US1] Refactor GetNextRace to use CacheProvider instead of NextRaceCache in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetNextRace.kt
- [X] T012 [P] [US1] Refactor GetRaceResults to use CacheProvider instead of RaceResultCache in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetRaceResults.kt
- [X] T013 [P] [US1] Refactor GetQualifyingResults to use CacheProvider instead of RaceResultCache in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetQualifyingResults.kt
- [X] T014 [P] [US1] Refactor GetSprintResults to use CacheProvider instead of RaceResultCache in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetSprintResults.kt
- [X] T015 [P] [US1] Refactor GetDriverStandings to use CacheProvider instead of DriverStandingsCache in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetDriverStandings.kt
- [X] T016 [P] [US1] Refactor GetConstructorStandings to use CacheProvider instead of ConstructorStandingsCache in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetConstructorStandings.kt
- [X] T017 [P] [US1] Refactor GetPersonalizedFeed to use CacheProvider (PERSONALIZED_FEED spec, 30s TTL) instead of internal ConcurrentHashMap in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetPersonalizedFeed.kt
- [X] T018 [US1] Update CircuitsModule to remove InMemoryCircuitCache binding, inject CacheProvider into GetCircuits in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/CircuitsModule.kt
- [X] T019 [P] [US1] Update ScheduleModule to remove cache bindings, inject CacheProvider into GetSchedule and GetNextRace in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/ScheduleModule.kt
- [X] T020 [P] [US1] Update RacesModule to remove cache binding, inject CacheProvider into GetRaceResults, GetQualifyingResults, GetSprintResults in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/RacesModule.kt
- [X] T021 [P] [US1] Update StandingsModule to remove cache bindings, inject CacheProvider into GetDriverStandings and GetConstructorStandings in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/StandingsModule.kt
- [X] T022 [P] [US1] Update FavoritesModule to inject CacheProvider into GetPersonalizedFeed in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/FavoritesModule.kt
- [X] T023 [P] [US1] Delete all 6 old cache interface files from src/main/kotlin/com/blaizmiko/f1backend/adapter/port/ (CircuitCache.kt, ScheduleCache.kt, DriverStandingsCache.kt, ConstructorStandingsCache.kt, NextRaceCache.kt, RaceResultCache.kt)
- [X] T024 [P] [US1] Delete all 6 InMemory cache implementation files from src/main/kotlin/com/blaizmiko/f1backend/infrastructure/cache/ (InMemoryCircuitCache.kt, InMemoryScheduleCache.kt, InMemoryDriverStandingsCache.kt, InMemoryConstructorStandingsCache.kt, InMemoryNextRaceCache.kt, InMemoryRaceResultCache.kt)
- [X] T025 [US1] Delete CacheEntry.kt from src/main/kotlin/com/blaizmiko/f1backend/domain/model/CacheEntry.kt
- [X] T026 [US1] Verify all existing tests pass and fix any compilation or test failures caused by the migration

**Checkpoint**: All endpoints return identical responses. Zero custom cache implementations remain. All tests pass. `./gradlew ktlintCheck detekt test` passes.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup

- [X] T027 Verify no runBlocking calls remain in cache-related code paths (search entire codebase)
- [X] T028 Verify no references to deleted cache interfaces or implementations remain anywhere in the codebase
- [X] T029 Run full quality check: `./gradlew ktlintCheck detekt test`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Phase 2 completion — core migration work
- **Polish (Phase 4)**: Depends on Phase 3 completion

### User Story Dependencies

- **US2 + US3 (Foundational)**: No dependencies on other stories — build CacheSpec + CacheRegistry
- **US1 (P1)**: Depends on US2 + US3 being complete (needs CacheRegistry to exist)

### Within Phase 3 (US1 Migration)

- T009–T017 (use case refactors): Can proceed in any order. T012–T017 are parallelizable.
- T018–T022 (DI module updates): Can proceed in parallel, but each depends on its corresponding use case refactor.
- T023–T025 (deletions): MUST happen AFTER all use case refactors and DI updates are complete.
- T026 (verification): MUST be last in Phase 3.

### Parallel Opportunities

- T003, T004 can run in parallel (Phase 2 — different files)
- T012, T013, T014, T015, T016, T017 can run in parallel (Phase 3 — different use case files)
- T019, T020, T021, T022 can run in parallel (Phase 3 — different DI module files)
- T023, T024 can run in parallel (Phase 3 — different directories)

---

## Parallel Example: Phase 3 Use Case Migration

```bash
# Launch all independent use case refactors together:
Task: "Refactor GetRaceResults in usecase/GetRaceResults.kt"
Task: "Refactor GetQualifyingResults in usecase/GetQualifyingResults.kt"
Task: "Refactor GetSprintResults in usecase/GetSprintResults.kt"
Task: "Refactor GetDriverStandings in usecase/GetDriverStandings.kt"
Task: "Refactor GetConstructorStandings in usecase/GetConstructorStandings.kt"

# Launch all DI module updates together (after use case refactors):
Task: "Update ScheduleModule in di/ScheduleModule.kt"
Task: "Update RacesModule in di/RacesModule.kt"
Task: "Update StandingsModule in di/StandingsModule.kt"

# Launch deletions together (after DI updates):
Task: "Delete cache interfaces in adapter/port/"
Task: "Delete InMemory implementations in infrastructure/cache/"
```

---

## Implementation Strategy

### MVP First (Phase 1 + 2 + 3)

1. Complete Phase 1: Setup (add dependencies)
2. Complete Phase 2: Foundational (CacheSpec + CacheRegistry + CacheModule + unit test)
3. Complete Phase 3: US1 — Migrate all use cases, delete old files
4. **STOP and VALIDATE**: All existing tests pass, all endpoints work identically
5. This delivers the core value: centralized config, non-blocking caches, bounded memory

### Single Developer Strategy

Execute phases sequentially: 1 -> 2 -> 3 -> 4. Within Phase 3, migrate use cases one at a time (start with GetCircuits as simplest, end with GetRaceResults as most complex).

---

## Notes

- Each use case refactor follows the same pattern: replace old cache port with CacheProvider, use Aedile `cache.get(key) { fetch() }`, keep retry throttle and stale-on-error via CacheProvider fallback methods
- GetCircuits is the simplest use case (single-value cache, no season key) — refactor it first as a template
- GetRaceResults/GetQualifyingResults/GetSprintResults share the same pattern (season+round keyed) — refactor one, copy pattern to others
- SeasonCache<T> is preserved (used for response building beyond caching)
- Schedule TTL continues to come from JolpicaConfig.cacheTtlHours — CacheSpec holds a default, CacheRegistry accepts an override
- Commit after each task or logical group
